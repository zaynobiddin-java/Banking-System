# Banking System

`Banking System` bu Spring Boot asosida yozilgan bank backend loyihasi bo'lib, foydalanuvchi autentifikatsiyasi, hisoblar, kartalar, tranzaksiyalar va kredit jarayonlarini boshqarish uchun mo'ljallangan.

Loyiha hozircha backend API ko'rinishida ishlaydi va Swagger UI orqali brauzerdan test qilish mumkin.

## Tezkor boshlash

Quyidagi buyruqlar bilan loyihani tez ishga tushirishingiz mumkin:

### H2 (standart, tez test uchun)

```powershell
.\gradlew.bat bootRun
```

### PostgreSQL (`banking_system` bilan)

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Swagger UI:

- `http://localhost:8080/docs`

## Mundarija

- [Asosiy imkoniyatlar](#asosiy-imkoniyatlar)
- [Texnologiyalar](#texnologiyalar)
- [Loyihaning tuzilmasi](#loyihaning-tuzilmasi)
- [Ishga tushirish](#ishga-tushirish)
- [Testlarni ishga tushirish](#testlarni-ishga-tushirish)
- [Swagger orqali tekshirish](#swagger-orqali-tekshirish)
- [API bo'limlari](#api-bolimlari)
- [Xavfsizlik](#xavfsizlik)

## Asosiy imkoniyatlar

- JWT asosidagi autentifikatsiya va token yangilash
- Foydalanuvchi uchun hisob ochish va hisoblarni boshqarish
- Kartalar yaratish, bloklash va qayta faollashtirish
- Hisoblar va kartalar o'rtasida tranzaksiyalar
- Kredit kalkulyatsiyasi
- Kredit arizasi yuborish, tasdiqlash va rad etish
- Kredit to'lov jadvali va kredit to'lovlarini amalga oshirish
- Swagger UI orqali API hujjatlari

## Texnologiyalar

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT (`jjwt`)
- OpenAPI / Swagger UI
- H2 Database
- PostgreSQL
- Gradle Kotlin DSL
- Lombok

## Loyihaning tuzilmasi

Asosiy paket:

`com.bankingsystem`

Modullar:

- `config` - umumiy bean va Swagger konfiguratsiyasi
- `controller` - REST endpointlar
- `dto` - request/response modellar
- `entity` - JPA entity va enumlar
- `exception` - xatolar va global handler
- `repository` - ma'lumotlar bazasi repository'lari
- `security` - JWT va Spring Security sozlamalari
- `service` - biznes logika

Asosiy ishga tushirish klassi:

- `com.bankingsystem.BankingSystemApplication`

## Talablar

- JDK 17
- IntelliJ IDEA yoki boshqa Java IDE
- Internetga ulangan muhit (birinchi marta dependency yuklash uchun)

## Ishga tushirish

### 1. Standart rejim: H2 bilan

Loyiha standart holatda ichki `H2` ma'lumotlar bazasi bilan ishga tushadi. Bu eng oddiy test rejimi.

```powershell
.\gradlew.bat bootRun
```

Yoki IntelliJ ichida:

1. `BankingSystemApplication` faylini oching
2. `main` yonidagi Run tugmasini bosing

Standart manzil:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Muhim:

- H2 `in-memory` ishlaydi
- ilova to'xtatilsa, ma'lumotlar o'chadi

### 2. PostgreSQL bilan ishga tushirish

PostgreSQL uchun alohida profil mavjud:

- `src/main/resources/application-postgres.yaml`

Ishga tushirish:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Agar IntelliJ ishlatsangiz:

1. `Run -> Edit Configurations`
2. `Active profiles` maydoniga `postgres` yozing

Standart PostgreSQL sozlamalari:

- host: `localhost`
- port: `5432`
- database: `banking_system`
- username: `postgres`
- password: `123`

Agar sizda boshqa ma'lumotlar bo'lsa, `application-postgres.yaml` ichida mos ravishda o'zgartiring.

## Testlarni ishga tushirish

```powershell
.\gradlew.bat clean test
```

## Swagger orqali tekshirish

Swagger UI manzili:

- `http://localhost:8080/docs`

Swagger orqali:

- endpointlarni ko'rish
- request body yuborish
- JWT token bilan avtorizatsiya qilish
- javoblarni brauzerda tekshirish mumkin

JWT kerak bo'ladigan endpointlar uchun avval:

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `accessToken` ni oling
4. Swagger'dagi `Authorize` tugmasi orqali quyidagini kiriting:

```text
Bearer <access_token>
```

## API bo'limlari

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

### Account

- `POST /api/accounts/user/{userId}`
- `GET /api/accounts/user/{userId}`
- `GET /api/accounts/{accountId}`
- `PUT /api/accounts/{accountId}`
- `PATCH /api/accounts/{accountId}/freeze`
- `PATCH /api/accounts/{accountId}/unfreeze`
- `DELETE /api/accounts/{accountId}`

### Card

- `POST /api/cards/account/{accountId}`
- `GET /api/cards/account/{accountId}`
- `GET /api/cards/{cardId}`
- `PATCH /api/cards/{cardId}/block`
- `PATCH /api/cards/{cardId}/unblock`

### Transaction

- `POST /api/transactions/card-transfer`
- `POST /api/transactions/deposit`
- `POST /api/transactions/withdraw`
- `POST /api/transactions/payment`
- `GET /api/transactions/history/{accountId}`

### Loan Application

- `POST /api/loan-applications`
- `GET /api/loan-applications/user/{userId}`
- `GET /api/loan-applications/pending`
- `POST /api/loan-applications/{id}/approve`
- `POST /api/loan-applications/{id}/reject`

### Loan

- `POST /api/loans/calculate`
- `GET /api/loans/user/{userId}`
- `GET /api/loans/{loanId}`
- `GET /api/loans/{loanId}/schedule`
- `POST /api/loans/{loanId}/pay`

## Namuna request'lar

### Ro'yxatdan o'tish

```json
{
  "fullName": "Ali Valiyev",
  "phone": "+998901234567",
  "password": "123456",
  "passport": "AA1234567",
  "pinfl": "12345678901234"
}
```

### Login

```json
{
  "phone": "+998901234567",
  "password": "123456"
}
```

### Hisob ochish

```json
{
  "currency": "UZS"
}
```

### Karta ochish

```json
{
  "type": "UZCARD"
}
```

### Karta orqali o'tkazma

```json
{
  "fromCardNumber": "8600123412341234",
  "toCardNumber": "9860123412341234",
  "amount": 50000
}
```

### Hisoblar o'rtasida to'lov

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150000
}
```

### Kredit hisoblash

```json
{
  "amount": 10000000,
  "durationMonth": 24,
  "loanType": "CONSUMER"
}
```

### Kredit arizasi yuborish

```json
{
  "userId": 1,
  "accountId": 1,
  "amount": 20000000,
  "durationMonth": 36,
  "loanType": "AUTO",
  "monthlyIncome": 8000000
}
```

## Enum qiymatlar

### CardType

- `UZCARD`
- `HUMO`
- `VISA`
- `MASTERCARD`

### LoanType

- `CONSUMER`
- `MICRO`
- `MORTGAGE`
- `AUTO`
- `BUSINESS`

### AccountStatus

- `ACTIVE`
- `FROZEN`
- `CLOSED`

### LoanStatus

- `ACTIVE`
- `CLOSED`
- `OVERDUE`
- `REJECTED`

## Xavfsizlik

- `/api/auth/**` ochiq
- `/docs`, `/swagger-ui/**`, `/v3/api-docs/**` ochiq
- qolgan endpointlar JWT talab qiladi

JWT sozlamalari:

- access token muddati: `120000 ms` (`2 daqiqa`)
- refresh token muddati: `604800000 ms` (`7 kun`)

## Rollar haqida

Loyihada 2 ta asosiy rol mavjud:

- `USER`
- `ADMIN`

Ro'yxatdan o'tgan foydalanuvchi standart holatda `USER` bo'lib yaratiladi.

Agar siz administratorga mo'ljallangan endpointlarni sinamoqchi bo'lsangiz, foydalanuvchi rolini ma'lumotlar bazasida `ADMIN` ga o'zgartirishingiz mumkin.

PostgreSQL uchun misol:

```sql
update _users
set role = 'ADMIN'
where phone = '+998901234567';
```

## Foydali eslatmalar

- Frontend uchun CORS hozircha `http://localhost:3000` ga ruxsat beradi
- Swagger UI test qilish uchun eng qulay usul
- Standart H2 rejimi tez test uchun qulay
- Doimiy ma'lumotlar bilan ishlash uchun `postgres` profildan foydalaning
