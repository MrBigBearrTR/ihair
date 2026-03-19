# iHair — Kuaför Randevu Yönetim Sistemi

Kuaför salonlarının müşteri, çalışan ve randevu süreçlerini yönetmek için geliştirilmiş RESTful API backend servisi.

---

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Veritabanı | PostgreSQL 18 |
| ORM | Hibernate 7 / Spring Data JPA |
| Bağlantı Havuzu | HikariCP |
| Web Sunucu | Apache Tomcat 11 (gömülü) |
| Yardımcı Kütüphane | Lombok |
| Test Veritabanı | H2 (geliştirme profili) |

---

## Gereksinimler

- Java 25+
- Maven 3.9+
- PostgreSQL 15+ (üretim)
- `ihair` adında bir PostgreSQL veritabanı

```sql
CREATE DATABASE ihair;
```

---

## Kurulum ve Çalıştırma

### Üretim (PostgreSQL)

```bash
mvn spring-boot:run
```

`application.properties` varsayılan bağlantı:

```
Host     : localhost:5432
Veritabanı: ihair
Kullanıcı : postgres
Şifre    : postgres
```

### Geliştirme (H2 — bellek içi)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

H2 konsol: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

---

## Veritabanı Şeması

Tablo ve kolon açıklamaları içeren DDL script'i:

```
src/main/resources/db/create_tables.sql
```

Hibrenate `ddl-auto=update` ile tabloları uygulama başlangıcında otomatik oluşturur. SQL script'ini **COMMENT ON** ifadelerini uygulamak için elle bir kez çalıştırın.

---

## Proje Yapısı

```
src/main/java/com/bigbear/ihair/
│
├── IhairApplication.java               ← Uygulama giriş noktası
│
├── common/
│   └── BaseEntity.java                 ← Tüm entity'lerin ortak tabanı
│                                         (id, createdAt, updatedAt — otomatik)
│
├── config/
│   └── WebConfig.java                  ← CORS yapılandırması (tüm origin'lere izin)
│
├── entity/
│   ├── Salon.java                      ← Kuaför salonu
│   ├── Employee.java                   ← Salon çalışanı
│   ├── Customer.java                   ← Müşteri
│   ├── HairService.java                ← Sunulan hizmet (kesim, boya vb.)
│   ├── Appointment.java                ← Randevu
│   └── enums/
│       └── AppointmentStatus.java      ← PENDING | CONFIRMED | CANCELLED | COMPLETED
│
├── repository/
│   └── CustomerRepository.java         ← Spring Data JPA repository
│
├── service/
│   ├── CustomerService.java            ← Servis arayüzü
│   └── impl/
│       └── CustomerServiceImpl.java    ← Servis implementasyonu
│
├── controller/
│   ├── CustomerController.java         ← /api/customers endpoint'leri
│   └── DenemeController.java           ← Test amaçlı geçici controller
│
├── dto/
│   └── response/
│       └── CustomerResponseDto.java    ← Müşteri yanıt modeli
│
└── exception/
    ├── GlobalExceptionHandler.java     ← Merkezi hata yönetimi
    └── ResourceNotFoundException.java  ← 404 hatası
```

---

## Veri Modeli

### İlişki Diyagramı

```
Salon ──< Employee ──< Appointment >── Customer
  │                         │
  └──< HairService >────────┘
```

| İlişki | Tür |
|---|---|
| Salon → Employee | Bire-Çok (1:N) |
| Salon → HairService | Bire-Çok (1:N) |
| Employee → Appointment | Bire-Çok (1:N) |
| Customer → Appointment | Bire-Çok (1:N) |
| HairService → Appointment | Bire-Çok (1:N) |

### Tablolar

#### `salons`
| Kolon | Tip | Açıklama |
|---|---|---|
| id | BIGSERIAL PK | Otomatik artan birincil anahtar |
| name | VARCHAR NOT NULL | Salonun ticari adı |
| address | VARCHAR | Açık adres |
| phone | VARCHAR | Telefon numarası |
| email | VARCHAR | E-posta adresi |
| created_at | TIMESTAMP | Oluşturma tarihi |
| updated_at | TIMESTAMP | Son güncelleme tarihi |

#### `employees`
| Kolon | Tip | Açıklama |
|---|---|---|
| id | BIGSERIAL PK | Otomatik artan birincil anahtar |
| first_name | VARCHAR NOT NULL | Ad |
| last_name | VARCHAR NOT NULL | Soyad |
| phone | VARCHAR | Telefon numarası |
| email | VARCHAR | E-posta adresi |
| salon_id | BIGINT FK | Bağlı salon |
| created_at | TIMESTAMP | Oluşturma tarihi |
| updated_at | TIMESTAMP | Son güncelleme tarihi |

#### `customers`
| Kolon | Tip | Açıklama |
|---|---|---|
| id | BIGSERIAL PK | Otomatik artan birincil anahtar |
| first_name | VARCHAR NOT NULL | Ad |
| last_name | VARCHAR NOT NULL | Soyad |
| phone | VARCHAR UNIQUE | Benzersiz telefon numarası |
| email | VARCHAR UNIQUE | Benzersiz e-posta adresi |
| active | BOOLEAN DEFAULT TRUE | Aktiflik durumu |
| notes | TEXT | Müşteri notları (tercih, alerji vb.) |
| created_at | TIMESTAMP | Oluşturma tarihi |
| updated_at | TIMESTAMP | Son güncelleme tarihi |

#### `hair_services`
| Kolon | Tip | Açıklama |
|---|---|---|
| id | BIGSERIAL PK | Otomatik artan birincil anahtar |
| name | VARCHAR NOT NULL | Hizmet adı |
| description | TEXT | Detaylı açıklama |
| price | NUMERIC(10,2) NOT NULL | Fiyat (TL) |
| duration_minutes | INTEGER NOT NULL | Tahmini süre (dakika) |
| salon_id | BIGINT FK | Bağlı salon |
| created_at | TIMESTAMP | Oluşturma tarihi |
| updated_at | TIMESTAMP | Son güncelleme tarihi |

#### `appointments`
| Kolon | Tip | Açıklama |
|---|---|---|
| id | BIGSERIAL PK | Otomatik artan birincil anahtar |
| customer_id | BIGINT FK | Randevu alan müşteri |
| employee_id | BIGINT FK | Randevuyu gerçekleştirecek çalışan |
| hair_service_id | BIGINT FK | Alınacak hizmet |
| appointment_date_time | TIMESTAMP NOT NULL | Randevu tarih ve saati |
| status | VARCHAR NOT NULL | Randevu durumu |
| notes | TEXT | Ek notlar |
| created_at | TIMESTAMP | Oluşturma tarihi |
| updated_at | TIMESTAMP | Son güncelleme tarihi |

**`AppointmentStatus` değerleri:**
- `PENDING` — Bekliyor (varsayılan)
- `CONFIRMED` — Onaylandı
- `CANCELLED` — İptal edildi
- `COMPLETED` — Tamamlandı

---

## Mevcut API Endpoint'leri

### Müşteriler

| Metot | URL | Açıklama |
|---|---|---|
| GET | `/api/customers/active` | Aktif müşterileri listeler |

### Yanıt Örneği — `GET /api/customers/active`

```json
[
  {
    "id": 1,
    "firstName": "Ali",
    "lastName": "Yılmaz",
    "phone": "05551112233",
    "email": "ali@mail.com",
    "active": true,
    "notes": "Saç boyasına alerjisi var",
    "createdAt": "2026-03-19T21:00:00"
  }
]
```

### Hata Yanıtı

```json
{
  "timestamp": "2026-03-19T21:00:00",
  "message": "Kaynak bulunamadı",
  "status": 404
}
```

---

## Geliştirme Yol Haritası

- [ ] Salon CRUD endpoint'leri
- [ ] Çalışan CRUD endpoint'leri
- [ ] Müşteri oluşturma / güncelleme endpoint'leri
- [ ] Hizmet CRUD endpoint'leri
- [ ] Randevu oluşturma ve durum güncelleme endpoint'leri
- [ ] Randevu çakışma kontrolü
- [ ] Kimlik doğrulama (Spring Security / JWT)
- [ ] Sayfalama ve filtreleme desteği
- [ ] Swagger / OpenAPI dokümantasyonu
