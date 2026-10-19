# Выкладка в RuStore — 7 приложений

Один кодбук → 7 приложений: 6 по разделам рубрикатора ОЭСР + полное (все рубрики).

| Приложение | package | Раздел |
|---|---|---|
| Медицина | `ru.cyberleninka.medical` | Медицинские науки |
| Естественные науки | `ru.cyberleninka.natural` | Естественные и точные науки |
| Техника и технологии | `ru.cyberleninka.engineering` | Техника и технологии |
| Гуманитарные науки | `ru.cyberleninka.humanities` | Гуманитарные науки |
| Сельское хозяйство | `ru.cyberleninka.agriculture` | Сельскохозяйственные науки |
| Социальные науки | `ru.cyberleninka.social` | Социальные науки |
| Полная библиотека | `ru.cyberleninka.full` | Все рубрики |

## 1. Ключи Appodeal
1. В кабинете Appodeal создайте **7 приложений** (6 разделов + полное), получите **App Key** каждого.
2. Скопируйте `ads.properties.example` → `ads.properties` (в корне проекта) и впишите ключи:
   `appodeal.medical`, `appodeal.natural`, `appodeal.engineering`, `appodeal.humanities`, `appodeal.agriculture`, `appodeal.social`, `appodeal.full`.
3. Если включена сеть **AdMob** — впишите и `admob.<flavor>` (App ID вида `ca-app-pub-...~...`).
4. `ads.properties` в `.gitignore` — в репозиторий не попадёт.

## 2. Подпись (обязательно для RuStore)
1. Создайте keystore:
   `keytool -genkeypair -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias key0`
2. Скопируйте `keystore.properties.example` → `keystore.properties` и заполните.
3. Соберите release:
   `gradlew :app:assembleRelease`
   Без `keystore.properties` release подписывается **debug-ключом** (годится только для тестов, для RuStore — нет).

## 3. Сборка APK
Для каждого раздела: `app/build/outputs/apk/<flavor>/release/app-<flavor>-release.apk`.
Готовые файлы также лежат в папке `dist/`.

## 4. Политика конфиденциальности (GitHub Pages)
Уже размещена: **https://andreyzagid-tech.github.io/cyberleninka/** (репозиторий `andreyzagid-tech/cyberleninka`, Pages: main `/docs`).
1. В карточке каждого из 7 приложений в RuStore укажите этот адрес как «Политика конфиденциальности».
2. При правках текста правьте `docs/index.html` и пушьте — страница обновится автоматически.
3. Адрес в приложении задан в `app/src/main/res/values/strings.xml` → `privacy_url` (при смене — поменять и пересобрать).

## 5. Чек-лист карточки в RuStore (для каждого из 6)
- [ ] Название (≤ 50 симв.)
- [ ] Категория: **Образование** (альтернатива — «Книги и справочники»)
3. Возрастной рейтинг: **12+** (из-за рекламы)
4. Короткое описание / полное описание (см. `medical.md`, `natural.md`, `engineering.md`, `humanities.md`, `agriculture.md`, `social.md`)
5. Теги (см. файлы)
5. Иконка 512×512 PNG (сейчас у всех одна — рекомендую сделать свою на каждый раздел)
6. Скриншоты: минимум 3, лучше 6–8 (портрет 1080×1920)
7. Фича-графика (по желанию)
7. Ссылка на политику конфиденциальности (п. 4)
8. Контактный e-mail
7. Загрузить APK, дождаться переподписи/модерации
8. При обновлениях увеличивать `versionCode` и `versionName` в `app/build.gradle.kts`

## 5. Реклама: что реализовано
- **Баннер** снизу: главный экран (Поиск/Рубрики/Библиотека) и экран рубрики.
- **Межстраничная**: при открытии статьи, не чаще 1 раза на 4 открытия.
- Инициализация в `MainActivity`; если `APPODEAL_KEY` пуст — реклама выключена, приложение работает как обычно.

## 6. Требования RuStore, которые уже учтены
- `targetSdk 35`, `minSdk 24`.
- Разрешения: только интернет/сеть/AD_ID; геолокация и прочее не запрашиваются.
- Политика конфиденциальности с описанием сбора рекламного идентификатора.
