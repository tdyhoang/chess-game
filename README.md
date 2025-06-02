## TODO:

- Nếu có gì cần lưu ý trong các tính năng này thì note ra đây để dễ theo dõi làm chung.
- Cập nhật tiến độ làm ở file này luôn nếu có thể (ít nhất là khi nào xong tính năng nào thì ghi lên đây).

### Đầu hàng

#### Gợi ý hướng làm

- Thêm `GameState` trong `Game`: `BLACK_WIN_RESIGN`, `WHITE_WIN_RESIGN`
- Viết method đầu hàng trong `ChessController`:
- Thao tác trên `this.gameModel` trong `ChessController` để cập nhật trạng thái ván cờ.
- Nếu `gameModel.GameState` là `Active`, `Check` thì mới cho phép đầu hàng.
- Kiểm tra `Game.currentPlayer` trong `Game` để biết người chơi hiện tại (người đã đầu hàng) là ai.
- Cập nhật `gameState` của `Game` thành `BLACK_WIN_RESIGN` hoặc `WHITE_WIN_RESIGN` tương ứng.
- Cập nhật các thông tin trên UI:
- `refreshBoardView();
  updateTurnLabel();
  updateStatusBasedOnGameState();`

### Ghi log ván cờ (DONE)

#### Tiến độ

- `NotationUtils` gần như hoàn thiện, việc xử lý trường hợp có nhiều quân cờ cùng loại có thể đi đến cùng 1 ô vẫn chưa
  triệt để, tuy nhiên cũng đã đủ dùng cơ bản.
- DONE: Tổng hợp lịch sử các nước đi thành PGN và lưu vào file.
- DONE: Đã implement `PgnFormatter` cho tính năng này
- TODO (optional): Lưu các thông tin khác của ván cờ vào PGN. Tính năng đánh giá và comment nước cờ.

#### Gợi ý hướng làm

- Lấy move từ `moveHistory` và gọi `getAlgebraicNotation()` để lấy chuỗi PGN tương ứng.

### Lưu và tải ván chơi

#### Tiến độ

- DONE: Đã xong tính năng lưu ván chơi vào pgn.

#### Gợi ý hướng làm

- Khi load: Đảm bảo các object/field sau đây có đầy đủ thông tin: `Board`, `Square`, `Piece`, `Player`, `Move` trong
  `moveHistory`, `positionHistoryCount`, `currentPositionHash`, `halfMoveClock`, `currentPlayer`, `gameState`.
- ->  Có thể bắt đầu bằng initialize bàn cờ ban đầu rồi lần lượt makeMove các nước theo thứ tự trong danh sách.
- Phía Controller, gọi 3 method sau khi load xong để cập nhật bàn cờ trên UI:
  `refreshBoardView();
  updateTurnLabel();
  updateStatusBasedOnGameState();`

### Lưu lịch sử chơi

### Chọn màu quân cờ

#### Gợi ý hướng làm

- Màu quân cờ được lưu trong `Player` (`PieceColor`).
- Đã thêm tính năng lật bàn cờ trong `ChessController`, dùng field `boardIsFlipped`.
