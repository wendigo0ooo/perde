# Perde

Kendi kaynağın, kendi ekranın.

Perde bir **oynatıcıdır**; yayın içeriği sağlamaz. Kullanıcının kendi M3U/M3U8
listesini (URL veya dosya) TV ve telefonda oynatır. Reklam, analitik ve hesap yoktur;
kanal listesi ve favoriler yalnız cihazda saklanır.

## v0.1.1
- M3U/M3U8 içe aktarma (HTTPS URL veya SAF dosya seçici), 64 MB sınırı
- Kanal listesi, arama, grup/kaynak bilgisi, favoriler
- Media3 ExoPlayer ile HLS oynatma, TV/kumanda uyumlu odak (D-pad)
- Android TV: LEANBACK_LAUNCHER, 320x180 banner, yatay oynatıcı
- Yalnız `INTERNET` + `ACCESS_NETWORK_STATE`; `allowBackup=false`; cleartext kapalı

## Derleme
GitHub Actions: `main` dalına push → debug + imzalı release APK ve GitHub Release.

## Yasal
Yalnızca hak sahibi olduğunuz, lisanslı veya kamu malı içerikleri ve kendi
listelerinizi kullanın. Uygulama DRM kırma, reklam/VPN engeli atlatma veya
korsan yayın sağlama amacı taşımaz.
