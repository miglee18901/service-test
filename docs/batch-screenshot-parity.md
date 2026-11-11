# Đối chiếu luồng batch với ảnh

Đã đối chiếu 12 ảnh trong cuộc hội thoại. Luồng vẫn chạy đồng bộ; chưa bổ sung job,
polling hay cơ chế loading vì yêu cầu hiện tại là khôi phục mã theo ảnh.

| Nhóm ảnh | File | Phần khôi phục |
| --- | --- | --- |
| 3 ảnh `executeBatch` | `OcsToolBatchExecutionService.java` | Chữ ký một tham số; `username_commandId`; kiểm tra token; map response/server; công thức số thread; `pageSize`, `begin`, `end`; tạo `ExecuteObj`; `addThreads`; phân loại kết quả; ghi log; dọn hai map trong `finally`. |
| 3 ảnh manager | `ThreadExecuteManager.java` | Singleton `getInstance()`; constructor private; pool 100; đúng kiểu bốn map; `synchronized addThreads`; `ExecutorCompletionService`; constructor worker hai tham số; nhận kết quả theo thứ tự hoàn thành; cộng bộ đếm; `checkUserIsRunningBatch`; các getter. |
| 6 ảnh callable | `MultiRestCallable.java` | Các field và constructor; `call`; bỏ qua map rỗng/sai số lượng; TPS với `ReentrantLock`/`Condition`; trạng thái và thời gian; `executeSingle`; URI/headers/HTTP/result code; catch unauthorized; clone tham số; parse code và convert XML. |

## Các phần không có đủ nguồn trong ảnh

- Dòng khởi tạo `tpsEntity` trước `List<ExecuteObj>` bị che: dùng một `TpsEntity` cho các worker của request.
- Không có thân `checkCondition`, `checkToken`, `writeLogToTextFile`, `getParamSetString`,
  `HttpClientUtil`, `ConnectionManager`, `OcsToolUtil`, các DTO và constants:
  bổ sung triển khai cục bộ để biên dịch và chạy mock. XML mock dùng phần tử
  `request` với `path`, `method`, `body`, `header name="..."`, placeholder `${name}`.
- Giá trị số constants không xuất hiện trong ảnh: mock dùng success `0`, fail `-1`,
  unauthorized `-2`, server không hợp lệ `-3`.
- `ConnectionManager.sentHttp` gọi mock, không gọi mạng. Hai lời gọi đặt/xóa context
  mock trong worker giúp giả lập từng `BatchParameter` mà vẫn giữ chữ ký HTTP trong ảnh.
- `BatchExecutionService` giữ vai trò adapter cho endpoint hiện có: lấy username từ
  `X-User`, gọi service gốc và ánh xạ HTTP status. Trường HTTP status không được serialize.
- Thêm `shutdown()` để đóng pool khi dừng ứng dụng hoặc kết thúc test.

## Những hành vi giữ nguyên theo ảnh

- `addThreads` giữ khóa `synchronized` cả lúc chờ worker hoàn thành.
- Key dùng dấu `_`; `checkUserIsRunningBatch` so sánh `key.split("_")[0]`.
- Chia nhóm bằng `pageSize = listParamSetOfServer.size() / numThread`, phần dư dồn vào
  thread cuối; vẫn lấy phần tử từ `listParamSetCurrent` như ảnh.
- Danh sách tham số server là `Map<Long, List<BatchParameter>>`, không phải map lồng;
  `finally` xóa cả entry server như ảnh.
- Worker bỏ qua map rỗng hoặc sai số lượng tham số; không kiểm tra tên bằng tập hợp.
- Catch lỗi dựng request vẫn gán `totalMsg = 0`; tổng lỗi batch cộng từ `failList.size()`.
- Kết quả được gom theo thứ tự worker hoàn thành; không sort lại theo index.
- Batch có một phần lệnh thất bại vẫn trả mã Success, kèm mô tả
  `BATCH_EXECUTE_NOT_SUCCESS_ALL`.

Các hành vi này khác bản triển khai trước và được giữ lại theo yêu cầu đối chiếu ảnh.
