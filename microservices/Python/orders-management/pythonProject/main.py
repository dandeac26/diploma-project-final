import asyncio
import websockets

async def connect():
    uri = "ws://127.0.0.1:8000/ws"  # Replace with your WebSocket server URL
    async with websockets.connect(uri) as websocket:
        while True:
            message = await websocket.recv()
            print(f"Received message: {message}")

# Run the connect function using asyncio
asyncio.run(connect())