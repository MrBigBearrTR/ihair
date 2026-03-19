# iHair — Kuaför Yönetim Sistemi

> Spring Boot tabanlı, JWT kimlik doğrulamalı RESTful API.  
> Salon, çalışan, müşteri, hizmet, randevu ve kampanya yönetimi sağlar.

---

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Veritabanı | PostgreSQL 18 |
| ORM | Hibernate 7 / Spring Data JPA |
| Güvenlik | Spring Security 7.0.3 + JWT (jjwt 0.12.6) |
| Yardımcı | Lombok |
| Derleme | Maven |

---

## Proje Yapısı

```
src/main/java/com/bigbear/ihair/
├── common/
│   └── BaseEntity.java              # id, createdAt, updatedAt
├── config/
│   └── SecurityConfig.java          # Spring Security + JWT filter konfigürasyonu
├── controller/
│   ├── AuthController.java
│   ├── SalonController.java
│   ├── EmployeeController.java
│   ├── CustomerController.java
│   ├── HairServiceController.java
│   ├── AppointmentController.java
│   └── CampaignController.java
├── dto/
│   ├── request/
│   │   ├── RegisterRequestDto.java
│   │   ├── LoginRequestDto.java
│   │   ├── RefreshTokenRequestDto.java
│   │   ├── SalonRequestDto.java
│   │   ├── EmployeeRequestDto.java
│   │   ├── CustomerRequestDto.java
│   │   ├── HairServiceRequestDto.java
│   │   ├── AppointmentRequestDto.java
│   │   └── CampaignRequestDto.java
│   └── response/
│       ├── AuthResponseDto.java
│       ├── ErrorResponseDto.java
│       ├── SalonResponseDto.java
│       ├── EmployeeResponseDto.java
│       ├── CustomerResponseDto.java
│       ├── HairServiceResponseDto.java
│       ├── AppointmentResponseDto.java
│       └── CampaignResponseDto.java
├── entity/
│   ├── enums/
│   │   ├── Role.java                # ADMIN, SALON_OWNER, EMPLOYEE, CUSTOMER
│   │   ├── AppointmentStatus.java   # PENDING, CONFIRMED, COMPLETED, CANCELLED
│   │   └── DiscountType.java        # PERCENTAGE, FIXED_AMOUNT, FREE_SESSION
│   ├── User.java
│   ├── RefreshToken.java
│   ├── Salon.java
│   ├── Employee.java
│   ├── Customer.java
│   ├── HairService.java
│   ├── Appointment.java
│   └── Campaign.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── BadRequestException.java
│   └── AppointmentConflictException.java
├── repository/
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── SalonRepository.java
│   ├── EmployeeRepository.java
│   ├── CustomerRepository.java
│   ├── HairServiceRepository.java
│   ├── AppointmentRepository.java
│   └── CampaignRepository.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
└── service/
    ├── AuthService.java / impl/AuthServiceImpl.java
    ├── SalonService.java / impl/SalonServiceImpl.java
    ├── EmployeeService.java / impl/EmployeeServiceImpl.java
    ├── CustomerService.java / impl/CustomerServiceImpl.java
    ├── HairServiceService.java / impl/HairServiceServiceImpl.java
    ├── AppointmentService.java / impl/AppointmentServiceImpl.java
    └── CampaignService.java / impl/CampaignServiceImpl.java
```

---

## Veritabanı Şeması

### Entity İlişkileri

```
Salon ──< Employee
Salon ──< HairService
Customer ──< Appointment
Employee ──< Appointment
HairService ──< Appointment
Campaign ──< Appointment
Customer ──o Campaign   (müşteriye özel kampanya)
```

### Tablolar

| Tablo | Önemli Alanlar |
|---|---|
| `users` | username, password, role |
| `refresh_tokens` | token, user_id, expires_at |
| `salons` | name, address, phone, email, **active** |
| `employees` | first_name, last_name, phone, email, salon_id, **active** |
| `customers` | first_name, last_name, phone, email, notes, **active** |
| `hair_services` | name, description, price, duration_minutes, salon_id, **active** |
| `appointments` | customer_id, employee_id, hair_service_id, appointment_date_time, **status**, campaign_id, final_price |
| `campaigns` | name, code (unique), discount_type, discount_value, max_usage_count, used_count, is_customer_specific, valid_from, valid_to, **active** |

> **Soft Delete:** Tüm entity'lerde fiziksel silme yoktur.  
> - `Salon`, `Employee`, `Customer`, `HairService`, `Campaign` → `active = false`  
> - `Appointment` → `status = CANCELLED`

---

## Kimlik Doğrulama (Auth)

- **Strateji:** Access Token + Refresh Token (Stateless JWT)  
- **Access Token Süresi:** 1 gün (86400000 ms)  
- **Refresh Token Süresi:** 7 gün  
- **Giriş Alanı:** `username` + `password`

### Endpoint'ler

| Method | URL | Açıklama | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Kullanıcı kaydı | - |
| POST | `/api/auth/login` | Giriş, token döner | - |
| POST | `/api/auth/refresh` | Access token yenile | - |
| POST | `/api/auth/logout` | Refresh token sil | Bearer |

### Login İsteği

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Login Yanıtı

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-03-20T10:00:00"
}
```

### Postman'da Token Kullanımı

1. `POST /api/auth/login` isteğini yapın.
2. Yanıttaki `accessToken` değerini kopyalayın.
3. Diğer isteklerde **Authorization** sekmesini açın.
4. **Type: Bearer Token** seçin, token'ı yapıştırın.

> **İpucu:** `postman/iHair_API_Collection.json` dosyasını import edin.  
> `Login` isteği çalıştırıldığında `accessToken` collection değişkenine otomatik kaydedilir.

---

## API Endpoint'leri

### Rol Bazlı Erişim

| Endpoint | ADMIN | SALON_OWNER | EMPLOYEE | CUSTOMER |
|---|:---:|:---:|:---:|:---:|
| `/api/salons/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/employees/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/hair-services/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/customers/**` | ✅ | ✅ | ✅ | ❌ |
| `/api/appointments/**` | ✅ | ✅ | ✅ | ❌ |
| `/api/campaigns/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/campaigns/validate` | ✅ | ✅ | ✅ | ✅ |

### Salon

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/salons` | Aktif salonları listele |
| GET | `/api/salons/{id}` | Salon detayı |
| POST | `/api/salons` | Yeni salon oluştur |
| PUT | `/api/salons/{id}` | Salon güncelle |
| DELETE | `/api/salons/{id}` | Salon pasifleştir |

### Çalışan (Employee)

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/employees?salonId=` | Aktif çalışanları listele (salon filtresi opsiyonel) |
| GET | `/api/employees/{id}` | Çalışan detayı |
| POST | `/api/employees` | Yeni çalışan ekle |
| PUT | `/api/employees/{id}` | Çalışan güncelle |
| DELETE | `/api/employees/{id}` | Çalışan pasifleştir |

### Müşteri (Customer)

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/customers` | Aktif müşterileri listele |
| GET | `/api/customers/{id}` | Müşteri detayı |
| POST | `/api/customers` | Yeni müşteri ekle |
| PUT | `/api/customers/{id}` | Müşteri güncelle |
| DELETE | `/api/customers/{id}` | Müşteri pasifleştir |

### Hizmet (Hair Service)

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/hair-services?salonId=` | Aktif hizmetleri listele (salon filtresi opsiyonel) |
| GET | `/api/hair-services/{id}` | Hizmet detayı |
| POST | `/api/hair-services` | Yeni hizmet ekle |
| PUT | `/api/hair-services/{id}` | Hizmet güncelle |
| DELETE | `/api/hair-services/{id}` | Hizmet pasifleştir |

### Randevu (Appointment)

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/appointments` | İptal edilmemiş randevuları listele |
| GET | `/api/appointments/{id}` | Randevu detayı |
| POST | `/api/appointments` | Yeni randevu oluştur |
| PUT | `/api/appointments/{id}` | Randevu güncelle |
| DELETE | `/api/appointments/{id}` | Randevuyu iptal et (CANCELLED) |

> **Çakışma Kontrolü:** Aynı çalışan için aynı tarih/saate ikinci bir randevu oluşturulmaya çalışıldığında `409 Conflict` hatası döner.

### Kampanya (Campaign)

| Method | URL | Açıklama |
|---|---|---|
| GET | `/api/campaigns` | Aktif kampanyaları listele |
| GET | `/api/campaigns/{id}` | Kampanya detayı |
| GET | `/api/campaigns/validate?code=` | Kampanya kodunu doğrula |
| POST | `/api/campaigns` | Yeni kampanya oluştur |
| PUT | `/api/campaigns/{id}` | Kampanya güncelle |
| DELETE | `/api/campaigns/{id}` | Kampanya pasifleştir |

> **Otomatik Kod:** `code` alanı boş bırakılırsa sistem `IH-XXXXXXXX` formatında benzersiz kod üretir.

---

## Kampanya Sistemi

### Discount Tipleri

| Tip | Açıklama | Örnek |
|---|---|---|
| `PERCENTAGE` | Yüzde indirim | %20 indirim |
| `FIXED_AMOUNT` | Sabit tutar indirimi | 100 TL indirim |
| `FREE_SESSION` | Ücretsiz seans | finalPrice = 0 |

### Kampanya Oluşturma — Örnek İstekler

**Yüzde indirim (kod belirtme):**
```json
{
  "name": "Kurucu Üye Kampanyası",
  "discountType": "PERCENTAGE",
  "discountValue": 20,
  "code": "KURUCU50",
  "maxUsageCount": 50,
  "validFrom": "2026-03-01T00:00:00",
  "validTo": "2026-12-31T23:59:59"
}
```

**Müşteriye özel (otomatik kod):**
```json
{
  "name": "VIP Müşteri İndirimi",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 100,
  "isCustomerSpecific": true,
  "customerId": 1,
  "maxUsageCount": 1
}
```

### Kampanyalı Randevu Oluşturma

```json
{
  "customerId": 1,
  "employeeId": 1,
  "hairServiceId": 1,
  "appointmentDateTime": "2026-04-01T10:00:00",
  "campaignCode": "KURUCU50"
}
```

Yanıtta `finalPrice` hesaplanmış indirimli fiyatı içerir.

---

## Hata Kodları

| HTTP Kodu | Durum | Senaryo |
|---|---|---|
| 200 | OK | Başarılı GET / PUT |
| 201 | Created | Başarılı POST |
| 204 | No Content | Başarılı DELETE |
| 400 | Bad Request | Geçersiz istek, süresi dolmuş kampanya |
| 401 | Unauthorized | Token eksik veya geçersiz |
| 403 | Forbidden | Yetersiz rol |
| 404 | Not Found | Kaynak bulunamadı veya pasif |
| 409 | Conflict | Mükerrer kayıt, randevu çakışması |
| 500 | Internal Server Error | Sunucu hatası |

### Hata Yanıtı Formatı

```json
{
  "timestamp": "2026-03-19T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Bu çalışan için 2026-04-01T10:00 saatinde zaten aktif bir randevu mevcut.",
  "path": "/api/appointments"
}
```

---

## Kurulum ve Çalıştırma

### Gereksinimler

- Java 25+
- Maven 3.9+
- PostgreSQL 18

### Veritabanı Yapılandırması

`application.properties` veya `application-dev.properties` içinde aşağıdaki değerleri ayarlayın:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ihair
spring.datasource.username=postgres
spring.datasource.password=<şifreniz>
spring.jpa.hibernate.ddl-auto=update
jwt.secret=<en_az_64_karakter_gizli_anahtar>
jwt.access-token-expiration=86400000
jwt.refresh-token-expiration=604800000
```

### Çalıştırma

```bash
mvn spring-boot:run
```

Uygulama `http://localhost:8080` adresinde başlar.

---

## Postman Collection

`postman/iHair_API_Collection.json` dosyasını Postman'e import edin.

- Collection değişkenleri: `baseUrl`, `accessToken`
- `Login` isteği çalıştırıldığında `accessToken` otomatik olarak güncellenir
- Tüm endpoint'ler örnek request body'leriyle hazırdır
