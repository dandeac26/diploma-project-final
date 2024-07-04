import asyncio

import websocket


async def connect():

    uri = "ws://192.168.68.56:8000/ws"  # Replace with your WebSocket server URL

    async with websocket.connect(uri) as websocket:

        while True:

            message = await websocket.recv()

            print(f"Received message: {message}")


# Run the connect function using asyncio
asyncio.run(connect())
