# AuthPlugin — Paper 1.21.x

Sign tabanlı authentication plugin. Oyuncular kayıt ve giriş işlemlerini tabela (sign) arayüzü ile yapar.

## Özellikler

- **Kayıt**: Yeni oyunculara turuncu parlayan mürekkepli tabela çıkar, şifresini 1. satıra yazar → Bitti'ye basar
- **Giriş**: Kayıtlı oyunculara giriş tabelası çıkar, şifresini yazar → Bitti'ye basar
- **Oturum yönetimi**: 24 saatlik oturum, aynı IP'den otomatik giriş
- **IP değişikliği**: Farklı IP'den bağlanınca otomatik şifre isteme
- **Tam engelleme**: Giriş yapmadan hareket, etkileşim, sohbet yapılamaz
- **SHA-256 + Salt** ile güvenli şifre saklama

## Derleme

### GitHub Actions (Otomatik)
`main` veya `master` branch'e push yapınca otomatik derlenir. Artifact olarak indirebilirsin.

### Manuel Derleme (Java 21 gerekli)
```bash
# Gradle wrapper jar'ı oluşturmak için:
gradle wrapper --gradle-version 8.12

# Derlemek için:
./gradlew shadowJar

# Çıktı: build/libs/AuthPlugin-1.0.0.jar
```

## Kurulum

1. `AuthPlugin-1.0.0.jar` dosyasını `plugins/` klasörüne koy
2. Sunucuyu başlat
3. `plugins/AuthPlugin/config.yml` dosyasını düzenle
4. `/authreload` komutuyla yenile

## Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/changepassword <sifre>` | Şifre değiştir | Herkese açık (giriş gerekli) |
| `/authreload` | Config yenile | `authplugin.admin` |
| `/resetpassword <oyuncu>` | Oyuncunun şifresini sıfırla | `authplugin.admin` |

## Config

`config.yml` dosyasında tüm mesajlar, tabela metinleri ve oturum süresi özelleştirilebilir.

## Veri Dosyaları

- `plugins/AuthPlugin/players/<uuid>.yml` — Oyuncu şifre verileri
- `plugins/AuthPlugin/sessions.yml` — Aktif oturumlar

## Gereksinimler

- Paper 1.21.x (1.21.4+)
- Java 21+
