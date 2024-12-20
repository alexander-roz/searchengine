## **Search Engine**

### API 
* Индексация WEB-сайтов
* Получение статистических результатов индексации
* Лемматизация результатов
* Осуществление поиска в индексированном контенте

### Индексация WEB-сайтов
##### На основании списка сайтов, указанного в файле конфигурации application.yaml,
##### производится парсинг сайтов и внесение полученных данных в базу данных MySQL.

###
##### Запуск полной индексации — GET /api/startIndexing
##### Метод запускает полную индексацию всех сайтов или полную
##### переиндексацию, если они уже проиндексированы.
![indexing](https://github.com/user-attachments/assets/cd2ce246-93f7-4647-8501-e1d38d008e43)

### 
##### Добавление или обновление отдельной страницы — POST /api/indexPage
##### Метод добавляет в индекс или обновляет отдельную страницу, адрес
##### которой передан в параметре.

### Результаты индексации
#### Статистика — GET /api/statistics
#### Метод возвращает статистику и другую служебную информацию о
#### состоянии поисковых индексов и самого движка.

### Лемматизация результатов
#### Лемматизация производится с помощью библиотеки org.apache.lucene

### Осуществление поиска в индексированном контенте
#### Получение данных по поисковому запросу — GET /api/search
#### Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
#### Возможен поиск на отдельно выбранном сайте или на всех проиндексированных
![searching](https://github.com/user-attachments/assets/e738fd4c-5c49-47dd-82e4-edc30500d3ee)

### Использованные технологии
- Java
- MySQL
- Spring Boot
