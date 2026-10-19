# КиберЛенинка — читалки научных статей (Android)

Один кодбук → **6 отдельных приложений** (product flavors), по одному на каждый раздел рубрикатора ОЭСР. Каждое приложение — «библиотечная читалка» своего раздела: поиск, чтение полных текстов, закладки, история, оффлайн-база.

## Приложения (flavors)

| Приложение | applicationId | Раздел |
|---|---|---|
| Медицина | `ru.cyberleninka.medical` | Медицинские науки |
| Естественные науки | `ru.cyberleninka.natural` | Естественные и точные науки |
| Техника и технологии | `ru.cyberleninka.engineering` | Техника и технологии |
| Гуманитарные науки | `ru.cyberleninka.humanities` | Гуманитарные науки |
| Сельское хозяйство | `ru.cyberleninka.agriculture` | Сельскохозяйственные науки |
| Социальные науки | `ru.cyberleninka.social` | Социальные науки |

## Что умеет

- **Поиск** — `POST /api/search` (внутренний API сайта): заголовок, аннотация, авторы, год, журнал, полный текст.
- **Рубрики раздела** — обход только рубрик своего раздела, пагинация по 20 статей.
- **Чтение** — полный текст из `<div class="ocr" itemprop="articleBody">`; метаданные из `citation_*`. Текст сохраняется в оффлайн-базу.
- **Оффлайн-библиотека** — «Скачать все» строит индекс по рубрикам раздела (прогресс сохраняется, можно прервать).
- **Моя библиотека** — закладки, история, поиск по скачанному оффлайн.
- **PDF** — сохранение оригинального PDF, открытие через FileProvider.

## Реклама

Интеграция [Appodeal](https://appodeal.com) (баннер в списках + межстраничная при открытии статьи). Ключи в `ads.properties` (в `.gitignore`):

```properties
appodeal.medical=<App Key>
appodeal.natural=<App Key>
appodeal.engineering=<App Key>
appodeal.humanities=<App Key>
appodeal.agriculture=<App Key>
appodeal.social=<App Key>
# необязательно, если включена сеть AdMob:
admob.medical=ca-app-pub-...~...
```

Шаблон — `ads.properties.example`. Без ключа реклама выключена, приложение работает штатно.

## Сборка

Нужен JDK 17+ и Android SDK (platform android-35). Используйте wrapper:

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:assembleRelease
# один flavor:
.\gradlew :app:assembleMedicalRelease
```

APK: `app/build/outputs/apk/<flavor>/{debug,release}/app-<flavor>-<buildtype>.apk`.

## Подпись release

Задаётся в `keystore.properties` (шаблон — `keystore.properties.example`, файл в `.gitignore`). Без него release подписывается debug-ключом (только для тестов).

## Политика конфиденциальности / RuStore

- `docs/index.html` — политика конфиденциальности для GitHub Pages. Замените плейсхолдеры `__РАЗРАБОТЧИК__`, `__EMAIL__`, `__ДАТА__` и адрес в `privacy_url` (`app/src/main/res/values/strings.xml`).
- Ссылка на политику — в приложении: **О приложении → Политика конфиденциальности**.
- `store/` — описания и чек-лист выкладки в RuStore по каждому приложению.

## Структура

```
app/src/main/java/ru/cyberleninka/app/
  data/  Api.kt · Db.kt · Repository.kt · Models.kt (Categories.all) · Sections.kt (скоуп раздела)
  ads/   Ads.kt (init/banner/interstitial)
  ui/    MainActivity · SearchFragment · BrowseFragment · LibraryFragment · CategoryActivity · ReaderActivity
app/src/<flavor>/res/   (app_name, цвет иконки, foreground/mipmap)
docs/    политика конфиденциальности
store/   метаданные RuStore
```

## Заметки

- Тесты обращаются к живому API (нужна сеть): `.\gradlew testDebugUnitTest`. На Windows с не-ASCII путём unit-тесты могут не запускаться из-за обработки путей в JVM — используйте путь без кириллицы.
- Обход рубрик идёт в фоне внутри процесса: не выключайте приложение; между запросами пауза 400 мс.
