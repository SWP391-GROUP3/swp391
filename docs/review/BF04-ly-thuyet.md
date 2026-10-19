# Lý thuyết dùng trong code BF-04

> Giải thích **mọi công nghệ và annotation** xuất hiện trong code BF-04 Iter1. Mỗi mục gồm: **là gì**, **tác dụng**, **ví dụ trong code của mình**, và **bỏ đi thì sao**.
> Đọc kèm file **BF04-on-review.docx**. Cập nhật: 13/10/2026.

---

## 0. Bản đồ: một request đi qua những gì

Ví dụ Kế toán thêm đơn giá (`POST /api/royalty-rates`):

```
Trình duyệt / Postman
   │  HTTP POST + JSON {"baseSalary": 2500000, "effectiveFrom": "2027-01-01"}
   ▼
[Tomcat nhúng trong Spring Boot]  nhận HTTP
   ▼
[Jackson]  đổi JSON thành object RoyaltyRateCreateRequest
   ▼
[Bean Validation]  @Valid: kiểm @NotNull, @DecimalMin... (sai thì 400)
   ▼
RoyaltyRateController   (@RestController)
   ▼  gọi
RoyaltyRateService      (@Service, @Transactional: mở transaction)
   │  luật nghiệp vụ, gọi RoyaltyCalculator (Java thuần)
   ▼  gọi
RoyaltyRateRepository   (Spring Data JPA tự sinh code)
   ▼
[Hibernate]  đổi object RoyaltyRate thành câu INSERT INTO royalty_rates ...
   ▼
[JDBC driver mssql-jdbc]  gửi SQL tới SQL Server
   ▼
SQL Server  kiểm PK, FK, UNIQUE, CHECK rồi lưu
   ▲  kết quả đi ngược lên: entity, rồi DTO, rồi ApiResponse, rồi JSON (Jackson), rồi HTTP 201
```

| Công nghệ | Làm phần nào |
|---|---|
| Spring Boot | Khung chạy app, tự cấu hình mọi thứ, có sẵn web server |
| Spring MVC | Nhận HTTP, gọi đúng hàm controller |
| Jackson | Đổi JSON ↔ object Java |
| Bean Validation (Hibernate Validator) | Kiểm dữ liệu vào bằng annotation |
| Spring Data JPA | Tự viết code repository từ tên hàm |
| JPA / Hibernate | Đổi object ↔ dòng trong bảng, tự sinh SQL |
| Lombok | Tự sinh getter, setter, constructor lúc compile |
| JUnit 5 + Mockito | Viết và chạy unit test |
| Maven | Tải thư viện, compile, chạy test, chạy app |

---

## 1. Spring Boot và Spring Core

### 1.1 Spring Boot là gì
- **Spring** là bộ khung (framework) Java để làm ứng dụng server.
- **Spring Boot** là Spring kèm **tự cấu hình** (auto-configuration):
  - Thấy có thư viện JPA và cấu hình DB là tự tạo kết nối.
  - Thấy có thư viện web là tự chạy **Tomcat nhúng** ở cổng 8080.
  - Mình không phải viết file cấu hình XML dài.
- **Starter** là gói thư viện theo chủ đề: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-test`.
- Điểm chạy app là `KpiApplication` có `@SpringBootApplication`. Annotation này gộp 3 việc: đánh dấu cấu hình, bật tự cấu hình, và **quét** (component scan) toàn bộ package `com.swp391.kpi` để tìm các class có `@RestController`, `@Service`...

### 1.2 Bean, IoC container, Dependency Injection
- **Bean** là object do **Spring tạo và quản lý**, không phải do mình `new`.
- **IoC container** (Inversion of Control) là "kho" chứa các bean. Thay vì mình tự tạo object, Spring tạo hộ, rồi **đưa** (inject) cho class nào cần.
- **Dependency Injection (DI):** class khai báo "tôi cần X", Spring đưa X vào.

```java
@Service
@RequiredArgsConstructor
public class RoyaltyRateService {
    private final RoyaltyRateRepository royaltyRateRepository;      // "tôi cần repository"
    private final SystemParameterService systemParameterService;    // "tôi cần service tham số"
}
```
Lombok `@RequiredArgsConstructor` sinh constructor nhận 2 field `final` này. Spring thấy constructor đó thì tự lấy 2 bean tương ứng truyền vào. Đây gọi là **constructor injection**.

**Vì sao không dùng `@Autowired` trên field** (AGENTS.md 3.2):
- Field có `final` thì không ai gán lại được.
- Khi test, có thể tự truyền object giả vào constructor.
- Thiếu dependency thì báo lỗi ngay lúc khởi động.

### 1.3 Các annotation "đánh dấu vai trò" (stereotype)

| Annotation | Đặt ở | Tác dụng | Trong code |
|---|---|---|---|
| `@SpringBootApplication` | class `main` | Bật Spring Boot, quét package | `KpiApplication` |
| `@RestController` | controller | Bean xử lý HTTP; giá trị trả về tự đổi sang JSON | `RoyaltyRateController`... |
| `@Service` | service | Bean chứa nghiệp vụ | `RoyaltyRateService`... |
| (không cần) | repository | Interface kế thừa `JpaRepository` thì Spring tự tạo bean | `RoyaltyRateRepository` |
| `@RestControllerAdvice` | class bắt lỗi | Bean bắt lỗi chung cho mọi controller | `GlobalExceptionHandler` |

**`RoyaltyCalculator` cố ý không có annotation**, vì nó cần tham số từ DB truyền vào lúc tạo. Service tự `new RoyaltyCalculator(params)`. Nhờ vậy test bằng `new` mà không cần Spring.

---

## 2. REST API với Spring MVC

### 2.1 HTTP căn bản
- **Method** cho biết làm gì:
  - `GET`: đọc.
  - `POST`: tạo mới.
  - `PUT`: sửa.
  - `DELETE`: xoá.
- **URL** chỉ tài nguyên: `/api/royalty-rates` (danh từ số nhiều, kebab-case, AGENTS.md 3.7).
- **Header** là thông tin kèm theo, ví dụ `Content-Type: application/json`, `X-User-Id: 6`.
- **Body** là dữ liệu gửi lên, dạng JSON.
- **Status code** cho biết kết quả:

| Mã | Nghĩa | Trong code |
|---|---|---|
| 200 OK | Thành công | GET, PUT |
| 201 Created | Tạo mới thành công | POST đơn giá |
| 400 Bad Request | Dữ liệu sai định dạng | `@Valid` không qua |
| 404 Not Found | Không tìm thấy | `NotFoundException` |
| 409 Conflict | Vi phạm luật nghiệp vụ | `BusinessException` |
| 500 | Lỗi server | Lỗi không lường trước, ví dụ thiếu tham số hệ thống |

### 2.2 Annotation của controller

| Annotation | Tác dụng | Ví dụ |
|---|---|---|
| `@RequestMapping("/api/royalty-rates")` | Tiền tố URL chung cho cả class | đặt trên `RoyaltyRateController` |
| `@GetMapping` | Hàm xử lý `GET` | `getAllRates()` |
| `@PostMapping` | Hàm xử lý `POST` | `createRate(...)` |
| `@PutMapping("/{id}")` | Hàm xử lý `PUT`, `{id}` là biến trong URL | `updateFactor(...)` |
| `@PathVariable Integer id` | Lấy `{id}` trong URL | `/api/media-factors/1` cho ra `id = 1` |
| `@RequestBody` | Lấy body JSON và đổi thành object | `RoyaltyRateCreateRequest request` |
| `@RequestHeader("X-User-Id")` | Lấy giá trị header | `Integer currentUserId` (tạm thay cho đăng nhập) |
| `@RequestParam` | Lấy tham số sau dấu `?` | (bài luyện) `?year=2026` |
| `@Valid` | Chạy kiểm tra các annotation validate trong DTO | trước `@RequestBody` |

### 2.3 `ResponseEntity` và `ApiResponse`
```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Thêm đơn giá thành công", data));
```
- `ResponseEntity` cho mình **chọn status code** và body. `ResponseEntity.ok(...)` là trả 200.
- `ApiResponse` là **khung chung** của nhóm cho mọi API. Thành công thì `{ success, message, data }`, lỗi thì `{ success, message, errors }`. Frontend chỉ cần đọc một kiểu.
- `@JsonInclude(JsonInclude.Include.NON_NULL)` trên `ApiResponse`: field nào `null` thì không in ra JSON. Vì vậy khi thành công không thấy `errors`.

### 2.4 Jackson: JSON ↔ object
- Jackson đọc JSON rồi gọi **setter** để điền vào DTO, và gọi **getter** để in DTO ra JSON. Đó là lý do DTO cần `@Getter` / `@Setter`.
- `LocalDate` in ra dạng `"2027-01-01"`. Enum in ra tên, ví dụ `"OPEN"`.
- Tên field Java (`effectiveFrom`) chính là tên key JSON.

---

## 3. Kiểm tra dữ liệu vào (Bean Validation)

**Là gì:** đặt annotation lên field của Request DTO. `@Valid` sẽ kiểm tra **trước khi** vào service.

| Annotation | Kiểm gì | Trong code |
|---|---|---|
| `@NotNull(message = ...)` | Không được `null` (thiếu trong JSON) | `baseSalary`, `effectiveFrom`, `factor` |
| `@DecimalMin(value = "1")` | Số ≥ giá trị (dạng chuỗi, dùng cho `BigDecimal`) | lương cơ sở ≥ 1; hệ số ≥ 0.01 |
| `@DecimalMax` | Số ≤ giá trị | (bài luyện) hệ số ≤ 10 |
| `@Digits(integer = 18, fraction = 0)` | Tối đa bao nhiêu chữ số nguyên và thập phân | lương là số nguyên ≤ 18 chữ số, khớp `DECIMAL(18,0)` |
| `@Min` / `@Max` | Số nguyên trong khoảng | (code KPI: tháng 1–12) |
| `@NotBlank`, `@Size` | Chuỗi không rỗng, độ dài | (chưa dùng ở BF-04) |

**Sai thì chuyện gì xảy ra:**
1. Spring ném `MethodArgumentNotValidException`.
2. `GlobalExceptionHandler.handleValidation` bắt lỗi đó, gom `message` của từng ô.
3. Trả về **400**: `{"success": false, "message": "Dữ liệu không hợp lệ", "errors": {"baseSalary": "Lương cơ sở không được để trống"}}`.

**Kiểm ở DTO hay ở service?** (AGENTS.md 3.5)
- **Định dạng** (thiếu, âm, quá dài) thì dùng annotation ở DTO.
- **Luật nghiệp vụ** cần đọc DB (ví dụ "ngày phải sau đơn giá mới nhất") thì kiểm ở **service** và ném `BusinessException`.

---

## 4. Xử lý lỗi

| Lớp lỗi | Kế thừa | Ném khi | Trả về |
|---|---|---|---|
| `NotFoundException` | `RuntimeException` | Không tìm thấy id | 404 |
| `BusinessException` | `RuntimeException` | Vi phạm luật nghiệp vụ | 409 |
| `IllegalStateException` | (Java có sẵn) | Cấu hình sai (thiếu tham số hệ thống) | 500 |
| `IllegalArgumentException` | (Java có sẵn) | Gọi hàm với đầu vào vô lý (tổng % ≠ 100) | 500 |

- `@RestControllerAdvice` là class "bắt lỗi chung" cho mọi controller.
- `@ExceptionHandler(BusinessException.class)` đánh dấu hàm xử lý đúng loại lỗi đó.
- Nhờ vậy controller **không cần `try/catch`** (AGENTS.md 3.6): service cứ `throw`, lỗi tự bay lên handler.
- `RuntimeException` là lỗi **unchecked**, không bắt buộc khai báo `throws`. Ngược lại là **checked exception** (ví dụ `IOException`) thì phải `try/catch` hoặc khai báo `throws`.

```java
throw new BusinessException("Ngày hiệu lực phải sau ngày 01/01/2026 của đơn giá mới nhất.");
```

---

## 5. JPA, Hibernate và ORM

### 5.1 ORM là gì
**ORM (Object-Relational Mapping)** nối **object Java** với **bảng SQL**: một class là một bảng, một object là một dòng, một field là một cột. Mình làm việc với object, còn thư viện tự sinh SQL.

### 5.2 JPA, Hibernate, Spring Data JPA khác nhau thế nào

| | Là gì | Ví von |
|---|---|---|
| **JPA** (Jakarta Persistence API) | **Bản đặc tả**: bộ interface và annotation chuẩn (`@Entity`, `@Id`...), gói `jakarta.persistence` | Luật giao thông |
| **Hibernate** | **Thư viện cài đặt** JPA: thật sự sinh SQL, quản lý object | Chiếc xe chạy theo luật |
| **Spring Data JPA** | Lớp tiện ích **trên** JPA: viết interface repository là có sẵn code | Tài xế riêng: chỉ cần nói "đi đâu" |

Code mình chủ yếu dùng annotation **JPA** (`jakarta.persistence.*`), và một annotation riêng của **Hibernate** là `@Nationalized` (`org.hibernate.annotations`).

### 5.3 Annotation của entity

| Annotation | Tác dụng | Trong code | Bỏ đi thì sao |
|---|---|---|---|
| `@Entity` | Đánh dấu class ánh xạ một bảng | `RoyaltyRate`, `MediaFactor`, `RoyaltyPeriod`, `SystemParameter` | JPA không biết class này, repository báo lỗi |
| `@Table(name = "royalty_rates")` | Chỉ đúng tên bảng | mọi entity | Spring tự đoán `royalty_rate` (số ít), `validate` báo không có bảng |
| `@Id` | Đánh dấu khoá chính | `private Integer id;` | Entity bắt buộc phải có, thiếu là lỗi |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Để DB tự tăng id (`INT IDENTITY(1,1)`) | mọi entity | Phải tự gán id trước khi lưu |
| `@Enumerated(EnumType.STRING)` | Lưu enum bằng **tên** (`"OPEN"`) | `RoyaltyPeriod.status` | Mặc định là `ORDINAL`, lưu **số thứ tự** (0, 1, 2...). Chèn thêm hằng là dữ liệu cũ sai nghĩa, và không khớp CHECK |
| `@Nationalized` | Cột là `NVARCHAR` (Unicode, tiếng Việt có dấu) | `returnNote`, `paramValue` | Hibernate hiểu là `VARCHAR`, `validate` báo lệch kiểu |
| `@Column(name = ...)` | Đặt tên cột thủ công | **không cần dùng** | Spring tự đổi `camelCase` sang `snake_case`: `effectiveFrom` thành `effective_from` |
| `@ManyToOne(fetch = FetchType.LAZY)` | Quan hệ n–1 tới entity khác | **chưa dùng** (`User` chưa có) | Tạm để `Integer createdBy` (AGENTS.md 3.3) |

**Ghép kiểu SQL Server với kiểu Java** (sai là `validate` báo lỗi):

| SQL Server | Java | Ví dụ |
|---|---|---|
| `INT` | `Integer` | `id`, `createdBy` |
| `SMALLINT` | `Short` | `periodYear` |
| `TINYINT` | `Byte` | `periodMonth` |
| `DECIMAL(18,0)`, `DECIMAL(5,2)` | `BigDecimal` | `amount`, `factor` |
| `DATE` | `LocalDate` | `effectiveFrom` |
| `DATETIME2` | `LocalDateTime` | `lockedAt` |
| `VARCHAR` (mã, trạng thái) | `String` hoặc `enum` | `mediaKind`, `status` |
| `NVARCHAR` | `String` + `@Nationalized` | `returnNote` |

### 5.4 Cấu hình JPA trong `application.properties`

| Cấu hình | Giá trị | Ý nghĩa |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | Lúc khởi động, Hibernate **so** entity với bảng; lệch thì **dừng app**. Không tự sửa DB |
| | (`update`, `create`... **cấm**) | `update` tự thêm cột theo entity, 5 người sửa song song là DB mỗi máy một kiểu |
| `spring.jpa.open-in-view` | `false` | Hết hàm service là đóng phiên DB. Muốn dữ liệu gì thì phải lấy xong trong service rồi đổi sang DTO |
| `spring.profiles.active` | `local` | Đọc thêm `application-local.properties` (mật khẩu DB, **không commit**) |

### 5.5 Persistence context và vì sao vẫn gọi `save()`
- Trong một transaction, Hibernate giữ **"sổ theo dõi"** (persistence context) các entity đã đọc lên.
- Sửa field của entity đang được theo dõi thì lúc **commit** Hibernate **tự phát hiện thay đổi** (dirty checking) và chạy `UPDATE`.
- Trong `MediaFactorService.updateFactor`, entity lấy bằng `findById` đã được theo dõi, nên **không gọi `save` cũng được cập nhật**. Nhóm vẫn gọi `save` cho **rõ ràng, dễ đọc** (giống code mẫu KPI).
- Với object **mới tạo** bằng `new` (thêm đơn giá) thì **bắt buộc** `save()`, Hibernate mới chạy `INSERT`.

---

## 6. Spring Data JPA: Repository

### 6.1 `JpaRepository<Entity, KiểuId>`
```java
public interface RoyaltyRateRepository extends JpaRepository<RoyaltyRate, Integer> { ... }
```
Chỉ là **interface**, không có code. Spring tự tạo class cài đặt lúc chạy. Kế thừa xong là có sẵn:

| Hàm có sẵn | SQL tương ứng |
|---|---|
| `findAll()` | `SELECT * FROM bảng` |
| `findById(id)` trả `Optional` | `SELECT ... WHERE id = ?` |
| `save(entity)` | `INSERT` (id null) hoặc `UPDATE` (đã có id) |
| `delete(entity)`, `deleteById(id)` | `DELETE` |
| `existsById(id)`, `count()` | `SELECT COUNT...` |

### 6.2 Query method: Spring đọc **tên hàm** rồi sinh SQL

| Hàm trong code | SQL Spring sinh ra |
|---|---|
| `findAllByOrderByEffectiveFromDesc()` | `SELECT * FROM royalty_rates ORDER BY effective_from DESC` |
| `findFirstByOrderByEffectiveFromDesc()` | `SELECT TOP 1 * FROM royalty_rates ORDER BY effective_from DESC` |
| `findAllByOrderByMediaKindAsc()` | `SELECT * FROM media_factors ORDER BY media_kind ASC` |
| `findAllByOrderByPeriodYearDescPeriodMonthDesc()` | `... ORDER BY period_year DESC, period_month DESC` |
| `findByFlow(String flow)` | `SELECT * FROM system_parameters WHERE flow = ?` |
| (bài luyện) `findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(date)` | `SELECT TOP 1 ... WHERE effective_from <= ? ORDER BY effective_from DESC` |

**Các từ khoá hay gặp:**
- `findBy`, `existsBy`, `countBy`, `deleteBy`
- `First` / `Top`
- `And`, `Or`
- `LessThan`, `LessThanEqual`, `GreaterThan`, `Between`, `Like`, `IsNull`, `In`
- `OrderBy...Asc` / `Desc`

Tên phải **trùng tên field Java** (`effectiveFrom`), không phải tên cột.

### 6.3 `Optional<T>`: "có thể có, có thể không"
```java
Optional<RoyaltyRate> latest = royaltyRateRepository.findFirstByOrderByEffectiveFromDesc();
if (latest.isPresent() && ...) { latest.get() ... }      // có thì lấy

mediaFactorRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("..."));   // không có thì ném lỗi
```
`Optional` buộc mình **nghĩ tới trường hợp không có**, thay vì nhận `null` rồi bị `NullPointerException`.

### 6.4 `@Query`: tự viết câu truy vấn
```java
@Query(value = "SELECT COALESCE(SUM(amount), 0) FROM royalty_lines WHERE period_id = :periodId",
        nativeQuery = true)
BigDecimal sumLineAmountByPeriodId(@Param("periodId") Integer periodId);
```
- **JPQL** (mặc định) viết theo **tên entity và field**: `SELECT r FROM RoyaltyRate r`. Cần có entity.
- **`nativeQuery = true`** là SQL thật của SQL Server, viết theo **tên bảng và cột**. Dùng ở đây vì entity `RoyaltyLine` chưa có (Iter2).
- `:periodId` là **tham số đặt tên**. `@Param("periodId")` gắn nó với tham số Java. Truyền tham số kiểu này **chống SQL injection**, khác với cộng chuỗi.
- `COALESCE(SUM(amount), 0)`: kỳ chưa có dòng nào thì `SUM` ra `NULL`, `COALESCE` đổi thành 0.
- **N+1 query:** lấy N kỳ rồi mỗi kỳ chạy thêm câu truy vấn, thành 1 + 2N câu. Ít dữ liệu thì chấp nhận được, nhiều thì nên gom bằng `GROUP BY`.

---

## 7. Transaction: `@Transactional`

**Transaction** là nhóm thao tác DB **"được ăn cả, ngã về không"**: tất cả cùng thành công, hoặc lỗi giữa chừng thì **huỷ hết** (rollback).

| Cách dùng | Ý nghĩa | Trong code |
|---|---|---|
| `@Transactional` | Hàm có **ghi** DB. Gặp `RuntimeException` thì rollback | `createRate`, `updateFactor` |
| `@Transactional(readOnly = true)` | Hàm **chỉ đọc**: Hibernate bỏ dò thay đổi (nhẹ hơn), chặn ghi nhầm | `getAllRates`, `getAllPeriods`, `getValuesByFlow` |

- Đặt ở **tầng service** (AGENTS.md 3.2), vì một nghiệp vụ có thể gồm nhiều thao tác DB.
- Ví dụ: `createRate` ném `BusinessException` **trước** `save` thì không có gì được ghi. Nếu ném **sau** `save` thì `INSERT` cũng bị **rollback**.

---

## 8. Lombok

Lombok **tự sinh code lúc compile**. File `.class` có đủ getter, setter, nhưng file nguồn thì gọn.

| Annotation | Sinh ra | Trong code |
|---|---|---|
| `@Getter` | `getAmount()`, `getEffectiveFrom()`... | entity, DTO |
| `@Setter` | `setAmount(...)`... | entity, Request DTO |
| `@NoArgsConstructor` | Constructor rỗng `new RoyaltyRate()`; **JPA bắt buộc** có | entity |
| `@RequiredArgsConstructor` | Constructor nhận mọi field `final`; Spring dùng để inject | service, controller |
| `@Data` | getter + setter + `toString` + `equals` + `hashCode` | **Cấm dùng cho entity**: `toString`/`hashCode` duyệt quan hệ, dễ lặp vô hạn hoặc query thừa |

---

## 9. DTO: vì sao không trả thẳng entity

- **DTO (Data Transfer Object)** là object chỉ để **chở dữ liệu vào/ra API**.
- **Request DTO** (`RoyaltyRateCreateRequest`) chỉ chứa ô người dùng **được phép gửi**. Không có `id`, `amount`, `createdBy`, nên client không thể tự gán đơn giá hay giả người tạo.
- **Response DTO** (`RoyaltyRateResponse`) chỉ chứa thứ **cần hiện**, và có thể thêm số liệu tính từ bảng khác (`totalAmount` của kỳ).
- **Trả entity ra thẳng** thì lộ cột nội bộ, dính lỗi lazy-loading (`open-in-view=false`), và đổi DB là vỡ API.
- **Map tay** bằng `static from(entity)` (AGENTS.md 3.4), không dùng MapStruct/ModelMapper, để tự giải thích được từng dòng.

```java
public static RoyaltyRateResponse from(RoyaltyRate rate) {
    RoyaltyRateResponse response = new RoyaltyRateResponse();
    response.setId(rate.getId());
    ...
    return response;
}
```

---

## 10. Kiến thức Java dùng trong code

### 10.1 `BigDecimal`: số thập phân chính xác
- **Vì sao:** `double` lưu nhị phân **gần đúng**: `0.1 + 0.2 = 0.30000000000000004`. Tính tiền bằng `double` là sai. AGENTS.md **cấm** `double` cho tiền.
- **Tạo từ chuỗi:** `new BigDecimal("0.1")` ra đúng 0.1, còn `new BigDecimal(0.1)` ra 0.1000000000000000055...
- **Không dùng `+ - * /`**, phải gọi hàm, và hàm **trả về số mới** (bất biến, immutable):

| Hàm | Nghĩa | Lưu ý |
|---|---|---|
| `a.add(b)`, `a.subtract(b)` | cộng, trừ | phải gán lại: `total = total.add(x)` |
| `a.multiply(b)` | nhân | |
| `a.divide(b, 2, RoundingMode.HALF_UP)` | chia, giữ 2 số lẻ, cách làm tròn | thiếu tham số mà gặp phép chia vô hạn (1/3) là **ném lỗi** |
| `a.compareTo(b)` | âm / 0 / dương | so **giá trị**. Đừng dùng `equals`: `100` và `100.00` là **khác** với `equals` |
| `a.max(b)` | số lớn hơn | kẹp tối thiểu hệ số = 1 |
| `toPlainString()` | in dạng thường | không ra `9E+1` |

- **Scale** là số chữ số sau dấu phẩy. `DECIMAL(5,2)` có scale 2.
- **Cách làm tròn (`RoundingMode`):**
  - `HALF_UP`: từ 5 trở lên thì làm tròn lên (2.156 thành 2.16). Dùng cho **hệ số**.
  - `FLOOR`: luôn làm tròn **xuống** (1.233.180 thành 1.233.000). Dùng cho **tiền**, không trả dư.
  - `DOWN`: bỏ phần lẻ về phía 0. Với số dương thì giống `FLOOR`.

### 10.2 `enum` và `switch` mũi tên
```java
public enum RoyaltyPeriodStatus { OPEN, CLOSED, LOCKED, PAID }

return switch (nature) {
    case ORIGINAL -> natureRateOriginal;
    case TRANSLATION -> natureRateTranslation;
    ...
};
```
- **enum** là kiểu có **tập giá trị cố định**. Gõ sai tên là compile báo lỗi ngay, tốt hơn dùng chuỗi `"OPNE"`.
- **switch biểu thức** (Java 14+): không cần `break`; đủ mọi hằng thì không cần `default`, thiếu thì compile báo lỗi.

### 10.3 Ngày giờ `java.time`
- `LocalDate` chỉ có ngày. `LocalDateTime` có ngày và giờ.
- `a.isAfter(b)`, `a.isBefore(b)`: so ngày (luật "ngày hiệu lực phải **sau** đơn giá mới nhất").
- `LocalDate.parse("2026-01-01")`: đọc chuỗi ISO.
- `DateTimeFormatter.ofPattern("dd/MM/yyyy")` rồi `date.format(...)`: in ra `01/01/2026` trong thông báo lỗi.

### 10.4 Collection, generic, vòng lặp
- `List<RoyaltyRateResponse> result = new ArrayList<>();`: danh sách có thứ tự. `<>` (diamond) để Java tự suy kiểu.
- `Map<String, String>`: bảng tra "khoá → giá trị" (tham số hệ thống). `HashMap` là bản cài đặt.
- **Generic** `<T>`: nói rõ danh sách chứa kiểu gì, nên compile kiểm được kiểu. `ApiResponse<T>` dùng cho mọi loại `data`.
- **for-each:** `for (RoyaltyRate rate : list) { ... }`. AGENTS.md ưu tiên `for` + `if` thay vì stream lồng nhau.

### 10.5 Từ khoá hay gặp
| Từ khoá | Nghĩa | Ví dụ |
|---|---|---|
| `private` | Chỉ dùng trong class | field, hàm phụ `roundDown` |
| `public` | Ai cũng gọi được | hàm của service |
| `static` | Thuộc **class**, không thuộc object | hằng `ONE_HUNDRED`, hàm `from()` |
| `final` | Gán **một lần** | field được inject, field luật của `RoyaltyCalculator` |
| `static final` | **Hằng số**, đặt tên `UPPER_SNAKE` | `ROYALTY_FLOW = "BF-04"` |
| `throw` / `try-catch` | Ném lỗi / bắt lỗi | `readDecimal` bắt `NumberFormatException` |

---

## 11. Unit test: JUnit 5 và Mockito

### 11.1 Khái niệm
- **Unit test** kiểm **một hàm hoặc một class** riêng lẻ, chạy rất nhanh, **không cần DB**.
- **Integration test** chạy cả app với DB thật. Nhóm thay bằng gọi thử Postman (AGENTS.md 3.9).
- **TDD:** viết test trước, chạy thấy **đỏ** (RED), viết code cho test **xanh** (GREEN), rồi dọn code. Mình đã làm đúng như vậy cho `RoyaltyCalculator`.
- **Mẫu AAA:** **Arrange** (chuẩn bị), **Act** (gọi hàm), **Assert** (kiểm kết quả).

### 11.2 JUnit 5
| Thành phần | Tác dụng |
|---|---|
| `@Test` | Đánh dấu một hàm test |
| `assertEquals(mong đợi, thực tế)` | Hai giá trị phải bằng nhau |
| `assertThrows(Loại.class, () -> ...)` | Đoạn code **phải ném** đúng loại lỗi; trả lại lỗi để kiểm `getMessage()` |
| `assertDoesNotThrow(() -> ...)` | Đoạn code **không được** ném lỗi |
| `() -> ...` (lambda) | "Đoạn code để chạy sau", JUnit tự gọi và bắt lỗi |

So `BigDecimal` trong test: `assertEquals(0, new BigDecimal("234000").compareTo(actual))`, so theo giá trị.

### 11.3 Mockito: test service mà không cần DB
**Mock** là object **giả** của repository: mình "dạy" nó trả về gì, rồi kiểm service xử lý đúng không.

| Thành phần | Tác dụng | Trong `RoyaltyRateServiceTest` |
|---|---|---|
| `@ExtendWith(MockitoExtension.class)` | Bật Mockito cho class test | đặt trên class |
| `@Mock` | Tạo object giả | `royaltyRateRepository`, `systemParameterService` |
| `@InjectMocks` | Tạo object **thật** và nhét các mock vào constructor | `royaltyRateService` |
| `when(x.hàm()).thenReturn(giá trị)` | "Khi gọi hàm này thì trả về..." | đơn giá mới nhất là 01/01/2026 |
| `thenAnswer(invocation -> invocation.getArgument(0))` | Trả lại chính đối số được truyền vào | `save(rate)` trả lại `rate` |
| `any(RoyaltyRate.class)` / `any()` | Khớp đối số bất kỳ | |
| `verify(mock, never()).save(any())` | Kiểm hàm **không bao giờ** được gọi | ngày sai thì **không được lưu** |

### 11.4 Lệnh chạy
```bash
mvn -f server/pom.xml clean test
```
`clean` xoá thư mục `target/` cũ trước khi compile, tránh chạy nhầm file `.class` cũ (IDE đôi khi compile lệch).

---

## 12. Kiến thức cơ sở dữ liệu

| Khái niệm | Nghĩa | Ví dụ BF-04 |
|---|---|---|
| **Primary key (PK)** | Định danh duy nhất mỗi dòng | `id INT IDENTITY(1,1) PRIMARY KEY` |
| **IDENTITY(1,1)** | DB tự tăng, bắt đầu 1, bước 1 | mọi bảng BF-04 |
| **Khoá chính ghép** | PK gồm nhiều cột | `article_authors (article_id, user_id)` |
| **Foreign key (FK)** | Cột trỏ tới PK bảng khác; DB **chặn** giá trị không tồn tại | `royalty_rates.created_by` tới `users.id` |
| **UNIQUE** | Không trùng | `effective_from`; `(period_year, period_month)` |
| **CHECK** | Điều kiện trên **một dòng** | `amount > 0`; `status IN ('OPEN', ...)` |
| **NOT NULL / NULL** | Bắt buộc / được để trống | `articles.event_id` NULL = không thuộc sự kiện |
| **DEFAULT** | Giá trị khi không truyền | `status DEFAULT 'OPEN'` |
| **DECIMAL(p,s)** | Số chính xác: p chữ số, s chữ số sau dấu phẩy | tiền `(18,0)`, hệ số `(5,2)` |
| **VARCHAR / NVARCHAR** | Chuỗi ASCII / Unicode (tiếng Việt có dấu) | mã `VARCHAR`, ghi chú `NVARCHAR` |
| **DATE / DATETIME2** | Ngày / ngày + giờ | `effective_from` / `locked_at` |
| **INDEX** | Tăng tốc tìm kiếm | `ix_royalty_gradings_period` |
| **Quan hệ 1–n** | 1 dòng A ứng nhiều dòng B (FK ở B) | 1 kỳ có nhiều phiếu chấm |
| **Quan hệ n–n** | Cần **bảng trung gian** | bài ↔ tác giả qua `article_authors` |
| **Snapshot** | Chép giá trị lúc xảy ra, không JOIN về bảng gốc | `royalty_gradings.unit_price` |
| **Versioning** | Không sửa, thêm phiên bản mới | `royalty_rates`, `royalty_gradings.version_no` |
| **Chuẩn hoá** | Mỗi thông tin lưu **một chỗ**, tránh lặp | cấu hình sự kiện ở `events`, không lặp trong từng bài |

**Vì sao CHECK không kiểm được "tổng % tác giả = 100":** CHECK chỉ nhìn **một dòng**, còn tổng là luật của **nhiều dòng**. Vì vậy phải kiểm ở service.

---

## 13. Maven và cấu trúc project

- **Maven** quản lý thư viện (khai báo trong `server/pom.xml`, tự tải) và chạy các bước: compile, test, đóng gói.

| Lệnh | Làm gì |
|---|---|
| `mvn -f server/pom.xml compile` | Biên dịch |
| `mvn -f server/pom.xml clean test` | Xoá bản cũ, compile, chạy toàn bộ test |
| `mvn -f server/pom.xml spring-boot:run` | Chạy app ở cổng 8080 |

- **Cấu trúc theo tầng** (AGENTS.md 3.1): `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`, `config/`. File thuộc luồng nào thì nhận ra bằng **tiền tố tên** (`Royalty...` là BF-04).
- `src/main/java` là code chạy thật, `src/test/java` là code test (không đóng gói vào app).

---

## 14. Câu hỏi lý thuyết hay gặp

| Hỏi | Trả lời ngắn |
|---|---|
| JPA với Hibernate khác gì? | JPA là **đặc tả** (interface, annotation); Hibernate là **thư viện cài đặt** JPA, thật sự sinh SQL |
| Spring Data JPA làm gì thêm? | Viết interface repository là có sẵn CRUD; đặt tên hàm đúng quy tắc là có query |
| DI là gì, lợi ích? | Spring tạo và đưa dependency vào; class không tự `new`, dễ test (đưa mock vào), dễ thay thế |
| Sao dùng constructor injection? | Field `final`, không quên inject, test truyền tay được |
| `@Transactional` để làm gì? | Gom các thao tác DB thành một khối, lỗi thì rollback hết |
| `readOnly = true`? | Hàm chỉ đọc: nhẹ hơn, không ghi nhầm |
| Sao không trả entity ra API? | Lộ dữ liệu nội bộ, lỗi lazy-loading, đổi DB là vỡ API. Dùng DTO |
| `@Valid` chạy lúc nào? | Trước khi vào hàm controller; sai thì 400, không vào service |
| `EnumType.STRING` vs `ORDINAL`? | STRING lưu tên (an toàn, khớp CHECK); ORDINAL lưu số thứ tự (sai nghĩa khi thêm hằng) |
| `ddl-auto=validate` khác `update`? | validate chỉ **kiểm**; update **tự sửa DB** theo entity (nguy hiểm khi làm nhóm) |
| `Optional` để làm gì? | Bắt buộc xử lý trường hợp không có dữ liệu, tránh `NullPointerException` |
| JPQL khác SQL native? | JPQL viết theo entity/field; native viết theo bảng/cột của DB cụ thể |
| Mock là gì? | Object giả thay repository khi test, để test service không cần DB |
| Vì sao `BigDecimal` phải gán lại khi `add`? | Bất biến: `add` trả về số **mới**, không sửa số cũ |
