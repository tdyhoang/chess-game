## How to Run
1. Ensure you have Java 21 (or newer) installed.
2. Download the `.jar` file (e.g., `ChessGame-v1.0.0.jar`) from [**Releases**](https://github.com/tdyhoang/chess-game/releases/latest).
3. Execute the application using the following command: `java -jar <name-of-the-downloaded-jar-file>.jar`
4. *Example:* `java -jar ChessGame-v1.0.0.jar`

## TODO:

- Nếu có gì cần lưu ý trong các tính năng này thì note ra đây để dễ theo dõi làm chung.
- Cập nhật tiến độ làm ở file này luôn nếu có thể (ít nhất là khi nào xong tính năng nào thì ghi lên đây).

## Cầu hoà

### Gợi ý hướng làm

- Nút `offerDraw` đã có sẵn, xử lý trong hàm `handleOfferDraw()`.
- Khi 2 bên đồng ý hoà, cập nhật GameState thành `GameState.DRAW_BY_AGREEMENT`.
- Tham khảo luồng xử lý của bên đầu hàng để biết thêm chi tiết.

### Đánh online

#### Gợi ý hướng làm

- Chế độ chơi là GameMode.PLAYER_VS_PLAYER
- Cứ xem chỗ nào gọi `requestEngineMove()` thì ở đó có thể gửi nước đi ở chỗ tương ứng trong chế độ PvP.
- Tương tự, có thể tham khảo `requestEngineMove()` để biết phải làm gì khi nhận nước đi.
- Nhớ xử lý undo/redo/cầu hoà/đầu hàng

### Lưu lịch sử chơi