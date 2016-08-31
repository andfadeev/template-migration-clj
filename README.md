# template-migration-clj

Утилита для переноса биллинговых шаблонов из WebDav в новую архитектуру

## Cборка

Для сборки нужен [Leiningen](http://leiningen.org/)

Из корня проекта запустить:

```
#> lein uberjar
```

## Использование

Создать config.edn на основе config.edn.ex:

```
{:from-url "https://hh.ru" ;; откуда качаем файлы
 :to-url "http://localhost:9091" ;; куда грузим файл
 :file-path "templates.csv" ;; путь до csv файла с входными данными
 :proto-session-path "proto-session"  ;; путь до файла с proto-session
}
```

Структура csv файла с входными данными:
```
ext;type;billing-file-id;site-id;lang
```
Например:
```
odt;AGREEMENT;11373661;1;EN
odt;AGREEMENT;16389376;1;RU
docx;AGREEMENT;15169958;75;RU
odt;AGREEMENT_HRSPACE;16391573;1;RU
odt;BILL;16380731;20;RU
odt;BILL;16380731;21;RU
```

SQL для формирования входного файла:
```
select bf.ext || ';' || t.type || ';' || t.value || ';' || t.site_id || ';' || t.lang as line
from billing_file bf
join (
select 'BILL' as type, value::int, site_id, lang from translation 
where name = 'admin.templates.billVfsFileId'
union
select 'BILL_UA_WITHOUT_VAT' as type, value::int, site_id, lang from translation 
where name = 'admin.templates.bill.ua.oldVfsFileId'
union
select 'BILL_BY_INDIVIDUAL' as type, value::int, site_id, lang from translation 
where name = 'admin.templates.bill.by.individualVfsFileId'
union
select 'AGREEMENT' as type, value::int, site_id, lang from translation 
where name = 'admin.templates.agreementVfsFileId'
union
select 'AGREEMENT_HRSPACE' as type, value::int, site_id, lang from translation 
where name = 'admin.templates.agreement_jcVfsFileId'
) t on t.value = bf.billing_file_id
order by t.type, t.site_id, t.lang;
```

Если нужно залить шаблон на докер стенд, то нужно прокинуть тунель до рабочей машины из контейнера hh-bbo-webapp, а в настройках указать to-url: localhost:<порт на рабочей машине>
```
#> ssh -f -N -R 9091:172.17.0.17:9090 afadeev@10.208.24.175
#> ssh -f -N -R <порт на рабочей машине>:<ip контейнера>:9090 <имя пользователя>@<ip рабочей машины>
```

Запускать из командной строки в директории с файлом config.edn (из корня проекта)
```
#> java -jar target/template-migration-clj-0.1.0-SNAPSHOT-standalone.jar
```

