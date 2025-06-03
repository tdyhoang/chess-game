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

### ~~Ghi log ván cờ (DONE)~~

#### Tiến độ

- `NotationUtils` gần như hoàn thiện, việc xử lý trường hợp có nhiều quân cờ cùng loại có thể đi đến cùng 1 ô vẫn chưa
  triệt để, tuy nhiên cũng đã đủ dùng cơ bản.
- DONE: Tổng hợp lịch sử các nước đi thành PGN và lưu vào file.
- DONE: Đã implement `PgnFormatter` cho tính năng này
- TODO (optional): Lưu các thông tin khác của ván cờ vào PGN. Tính năng đánh giá và comment nước cờ.

#### Gợi ý hướng làm

- Lấy move từ `moveHistory` và gọi `getAlgebraicNotation()` để lấy chuỗi PGN tương ứng.

### ~~Lưu và tải ván chơi (DONE)~~

#### Tiến độ

- DONE: Đã xong tính năng lưu và tải ván chơi vào pgn.
- Có thể cải thiện thêm: Tải ván chơi có custom FEN (hiện tại chỉ mới tải được pgn dùng bàn cờ khởi đầu chuẩn).
- Có thể cải thiện thêm: Cho phép thay đổi thông tin ván cờ (PgnHeaders). Hiện tại chỉ dùng giá trị khởi tạo mặc định hoặc lấy từ file pgn đã lưu.

### Lưu lịch sử chơi

### Chọn màu quân cờ

#### Gợi ý hướng làm

- Màu quân cờ được lưu trong `Player` (`PieceColor`).
- Đã thêm tính năng lật bàn cờ trong `ChessController`, dùng field `boardIsFlipped`.
