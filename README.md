# Banking System

`Banking System` - bu Spring Boot asosida yozilgan REST API loyihasi bo'lib, bankning asosiy jarayonlarini boshqaradi: autentifikatsiya, hisoblar, kartalar, tranzaksiyalar, kredit arizalari va kredit to'lovlari.

Loyiha to'liq backend formatda ishlaydi va Swagger UI orqali test qilish mumkin.

## Mundarija

- [Asosiy imkoniyatlar](#asosiy-imkoniyatlar)
- [Texnologiyalar](#texnologiyalar)
- [Arxitektura va paketlar](#arxitektura-va-paketlar)
- [Talablar](#talablar)
- [Ishga tushirish](#ishga-tushirish)
- [Swagger va hujjatlar](#swagger-va-hujjatlar)
- [Autentifikatsiya va xavfsizlik](#autentifikatsiya-va-xavfsizlik)
- [API endpointlar](#api-endpointlar)
- [Transaction History filtrlari](#transaction-history-filtrlari)
- [Cron (scheduler) jarayonlari](#cron-scheduler-jarayonlari)
- [Domain modellar va enumlar](#domain-modellar-va-enumlar)
- [Testlar](#testlar)

## Asosiy imkoniyatlar

- JWT (`access` + `refresh`) asosida login tizimi
- Foydalanuvchilar uchun bank hisoblarini yaratish va boshqarish
- Kartalar chiqarish, bloklash, blokdan chiqarish
- Karta va hisoblar o'rtasida to'lov/o'tkazmalar
- Tranzaksiya tarixini filterlash (sana, summa, tur)
- Kredit kalkulyatsiyasi, kredit arizasi, tasdiqlash/rad etish
- Kredit to'lov grafigi va to'lovlarni qabul qilish
- Overdue kreditlar va muddati o'tgan kartalarni avtomatik qayta ishlash

## Texnologiyalar

- Java 17
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA (Hibernate)
- Spring Security + Method Security
- Bean Validation (`jakarta.validation`)
- JWT (`io.jsonwebtoken`)
- OpenAPI/Swagger (`springdoc-openapi`)
- H2 (in-memory) va PostgreSQL
- Lombok
- Gradle (Kotlin DSL)

## Arxitektura va paketlar

Asosiy paket: `com.bankingsystem`

- `config` - umumiy beanlar, autentifikatsiya provider, Swagger/OpenAPI konfiguratsiyasi
- `controller` - REST endpointlar
- `service` - biznes mantiq va jarayonlar
- `repository` - JPA repository qatlami
- `entity` - ma'lumotlar modeli va enumlar
- `dto` - request/response obyektlari
- `security` - JWT filtr va HTTP xavfsizlik qoidalari
- `exception` - maxsus xatolar va global exception handler

Ishga tushirish klassi: `com.bankingsystem.BankingSystemApplication`

## Talablar

- JDK 17
- Gradle wrapper (`gradlew`, `gradlew.bat`) mavjud
- (Ixtiyoriy) PostgreSQL 15+ agar `postgres` profil ishlatilsa

## Ishga tushirish

### 1) Standart rejim (H2, tez test uchun)

```powershell
.\gradlew.bat bootRun
```

### 2) PostgreSQL profili bilan

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Standart URL'lar:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

`postgres` profil uchun standart sozlamalar (`application-postgres.yaml`):

- host: `localhost`
- port: `5432`
- database: `banking_system`
- username: `postgres`
- password: `123`

## Swagger va hujjatlar

Swagger UI orqali barcha endpointlarni tekshirish mumkin:

- `http://localhost:8080/docs`

OpenAPI konfiguratsiyasida `Bearer JWT` xavfsizlik sxemasi sozlangan.

## Autentifikatsiya va xavfsizlik

### Ochiq endpointlar

- `/api/auth/**`
- `/docs`, `/docs/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- `/h2-console/**`

Qolgan barcha endpointlar autentifikatsiya talab qiladi.

### JWT sozlamalari

- `access-expiration`: `120000 ms` (2 daqiqa)
- `refresh-expiration`: `604800000 ms` (7 kun)

### Rollar

- `USER`
- `ADMIN`

`@PreAuthorize("hasRole('ADMIN')")` bilan himoyalangan admin endpointlar mavjud (hisob/karta boshqaruvi, pending kredit arizalari, approve/reject).

### CORS

Ruxsat etilgan origin:

- `http://localhost:3000`

## API endpointlar

### Auth (`/api/auth`)

- `POST /register`
- `POST /login`
- `POST /refresh`

### Account (`/api/accounts`)

- `POST /user/{userId}` - admin
- `GET /user/{userId}`
- `GET /{accountId}`
- `PUT /{accountId}` - admin
- `PATCH /{accountId}/freeze` - admin
- `PATCH /{accountId}/unfreeze` - admin
- `DELETE /{accountId}` - admin

### Card (`/api/cards`)

- `POST /account/{accountId}` - admin
- `GET /account/{accountId}`
- `GET /{cardId}`
- `PATCH /{cardId}/block` - admin
- `PATCH /{cardId}/unblock` - admin

### Transaction (`/api/transactions`)

- `POST /card-transfer`
- `POST /deposit`
- `POST /withdraw`
- `POST /payment`
- `GET /history/{accountId}`

### Loan Application (`/api/loan-applications`)

- `POST /`
- `GET /user/{userId}`
- `GET /pending` - admin
- `POST /{id}/approve` - admin
- `POST /{id}/reject` - admin

### Loan (`/api/loans`)

- `POST /calculate`
- `GET /user/{userId}`
- `GET /{loanId}`
- `GET /{loanId}/schedule`
- `POST /{loanId}/pay`

## Transaction History filtrlari

`GET /api/transactions/history/{accountId}` endpointi quyidagi ixtiyoriy query parametrlari bilan ishlaydi:

- `fromDate` (`yyyy-MM-dd`)
- `toDate` (`yyyy-MM-dd`)
- `minAmount`
- `maxAmount`
- `type` (`TRANSFER`, `PAYMENT`, `DEPOSIT`, `WITHDRAWAL`)

Misol:

`GET /api/transactions/history/1?fromDate=2026-04-01&toDate=2026-04-30&minAmount=10000&maxAmount=500000&type=PAYMENT`

Validatsiya qoidalari:

- `fromDate <= toDate`
- `minAmount >= 0`
- `maxAmount >= 0`
- `minAmount <= maxAmount`

## Cron (scheduler) jarayonlari

Loyihada `@EnableScheduling` yoqilgan va quyidagi avtomatik jarayonlar mavjud:

- `CardExpiryScheduler` - har kuni soat `00:00` da muddati o'tgan kartalarni `EXPIRED` qiladi
- `LoanOverdueScheduler` - har kuni soat `01:00` da overdue kredit to'lovlariga penya hisoblaydi va statuslarni yangilaydi

## Domain modellar va enumlar

### Asosiy entity'lar

- `User`
- `Account`
- `Card`
- `Transaction`
- `LoanApplication`
- `Loan`
- `LoanPayment`
- `Company`
- `Partner`

### Enum qiymatlari

- `Role`: `USER`, `ADMIN`
- `UserStatus`: `ACTIVE`, `BLOCKED`, `INACTIVE`
- `UserType`: `INDIVIDUAL`, `BUSINESS`
- `OwnerType`: `USER`, `COMPANY`, `SYSTEM`
- `PartnerType`: `PAYMENT_SYSTEM`, `BANK`, `FINTECH`, `GOVERNMENT`
- `AccountStatus`: `ACTIVE`, `FROZEN`, `CLOSED`
- `CardStatus`: `ACTIVE`, `BLOCKED`, `EXPIRED`
- `CardType`: `UZCARD`, `HUMO`, `VISA`, `MASTERCARD`
- `TransactionType`: `TRANSFER`, `PAYMENT`, `DEPOSIT`, `WITHDRAWAL`
- `TransactionStatus`: `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`
- `LoanType`: `CONSUMER`, `MICRO`, `MORTGAGE`, `AUTO`, `BUSINESS`
- `LoanApplicationStatus`: `PENDING`, `APPROVED`, `REJECTED`
- `LoanStatus`: `ACTIVE`, `CLOSED`, `OVERDUE`, `REJECTED`
- `LoanPaymentStatus`: `SCHEDULED`, `PAID`, `OVERDUE`, `PARTIAL`

## Testlar

Testlarni ishga tushirish:

```powershell
.\gradlew.bat clean test
```

Mavjud test sinflari:

- `BankingSystemApplicationTests` (context load testi)
- `ServiceSecurityAndValidationTests` (servis darajasidagi security/validation holatlari)

## Tezkor foydalanish bo'yicha eslatma

Swagger orqali test ketma-ketligi:

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `accessToken` ni oling
4. Swagger'da `Authorize` ga `Bearer <access_token>` kiriting
5. Himoyalangan endpointlarni test qiling
