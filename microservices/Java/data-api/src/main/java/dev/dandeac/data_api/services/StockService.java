package dev.dandeac.data_api.services;

import dev.dandeac.data_api.dtos.StockDTO;
import dev.dandeac.data_api.dtos.builders.StockBuilder;
import dev.dandeac.data_api.entity.Stock;
import dev.dandeac.data_api.entity.StockId;
import dev.dandeac.data_api.repositories.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockService {
    private final StockRepository stockRepository;
    private final ProviderService providerService;
    private final IngredientService ingredientService;

    private final StockBuilder stockBuilder;

    @Autowired
    public StockService(StockRepository stockRepository, ProviderService providerService, IngredientService ingredientService, StockBuilder stockBuilder){
        this.stockRepository = stockRepository;
        this.providerService = providerService;
        this.ingredientService = ingredientService;
        this.stockBuilder = stockBuilder;
    }

    public List<StockDTO> findStocks() {
        List<Stock> stockList = stockRepository.findAll();
        return stockList.stream()
                .map(stockBuilder::toStockDTO)
                .collect(Collectors.toList());
    }

    public StockDTO addStock(StockDTO stockDTO) {

        if (stockRepository.existsByIdIngredientIdAndIdProviderId(stockDTO.getIngredientId(), stockDTO.getProviderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock for product " + stockDTO.getIngredientId() + " already exists");
        }

        if (!ingredientService.existsById(stockDTO.getIngredientId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient with id " + stockDTO.getIngredientId() + " does not exist");
        }

        if (!providerService.existsById(stockDTO.getProviderId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider with id " + stockDTO.getProviderId() + " does not exist");
        }

        Stock stock = stockBuilder.toStock(stockDTO);
        stock.setIngredient(ingredientService.findById(stockDTO.getIngredientId()));
        stock.setProvider(providerService.findById(stockDTO.getProviderId()));
        Stock savedStock = stockRepository.save(stock);
        return stockBuilder.toStockDTO(savedStock);
    }

    public void deleteStock(String ingredientId, String providerId) {
        StockId id = new StockId(UUID.fromString(ingredientId), UUID.fromString(providerId));
        if (!stockRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock with product id " + ingredientId + " and ingredient id "+ providerId + " does not exist");
        }
        stockRepository.deleteById(id);
    }

    public void deleteProductStock(String ingredientId) {
        List<Stock> stocks = stockRepository.findByIdIngredientId(UUID.fromString(ingredientId));
        if (stocks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stocks with product id " + ingredientId + " do not exist");
        }
        stockRepository.deleteAll(stocks);
    }

    public StockDTO updateStock(StockId stockId, StockDTO stockDTO) {
        if (!stockRepository.existsById(stockId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock with id " + stockId + " does not exist");
        }

        Stock stock = stockBuilder.toStock(stockDTO);
        stock.setId(new StockId(stockDTO.getIngredientId(), stockDTO.getProviderId()));
        Stock updatedStock = stockRepository.save(stock);
        return stockBuilder.toStockDTO(updatedStock);
    }

    public List<StockDTO> findStockByIngredientId(String ingredientId) {
        List<Stock> stocks = stockRepository.findByIdIngredientId(UUID.fromString(ingredientId));
        return stocks.stream()
                .map(stockBuilder::toStockDTO)
                .collect(Collectors.toList());
    }

    public void deleteAllStocks() {
        stockRepository.deleteAll();
    }
    
    public StockDTO findStockById(StockId stockId) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock with id " + stockId + " does not exist"));
        return stockBuilder.toStockDTO(stock);
    }

    public List<StockDTO> findUniqueStocks() {
        List<Stock> stocks = stockRepository.findAll();
        return stocks.stream()
                .collect(Collectors.groupingBy(stock -> stock.getIngredient().getIngredientId()))
                .entrySet().stream()
                .map(entry -> {
                    StockDTO dto = new StockDTO();
                    dto.setIngredientId(entry.getKey());
                    // Fetching ingredient name
                    String ingredientName = ingredientService.findById(entry.getKey()).getName();
                    dto.setIngredientName(ingredientName);

                    int totalQuantity = entry.getValue().stream().mapToInt(Stock::getQuantity).sum();
                    int totalMaxQuantity = entry.getValue().stream().mapToInt(Stock::getMaxQuantity).sum();
                    double averageQuantityPerPackage = entry.getValue().stream().mapToInt(Stock::getQuantityPerPackage).average().orElse(0);
                    double averagePrice = entry.getValue().stream().mapToDouble(Stock::getPrice).average().orElse(0);

                    // Determining the most frequent packaging
                    String mostFrequentPackaging = entry.getValue().stream()
                            .collect(Collectors.groupingBy(stock -> stock.getIngredient().getPackaging(), Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    dto.setQuantity(totalQuantity);
                    dto.setMaxQuantity(totalMaxQuantity);
                    dto.setQuantityPerPackage((int) averageQuantityPerPackage);
                    dto.setPrice(averagePrice);
                    dto.setPackaging(mostFrequentPackaging);

                    return dto;
                })
                .collect(Collectors.toList());
    }
}
