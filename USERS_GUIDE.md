# Миграционный Сервис



## Для старта работы необходимо:

1) В [application.yml](src%2Fmain%2Fresources%2Fapplication.yml) зарегестрировать datasources (указать : url, username, password, driver-class-name).
2) В [application.yml](src%2Fmain%2Fresources%2Fapplication.yml) указать путь до export папки, где будут храниться и удаляться экспорт архивы.
3) При запуске сервиса происходит создание таблиц и заполнение  
4) В [requests.txt](src%2Fmain%2Fresources%2Fpostman%2Frequests.txt) файле есть запросы для сервиса

## Начало работы с сервисом:
1) EXPORT Создание задачи по созданию экспорт файла  POST: /public/task/export/{указывам datasourcesName из которого хотим экспортировать данные}/{fileScriptName}
   В таблице migration_task созданётся сущность по данной задачи с guid и статусом in progress.
   А также асинхронно начниается формирование архива с данными по данному скрипту в папке export
2) По запросу GET: /public/export/migration-task/{taskGuid} в случае если задача ещё не выполнена вернётся оповещение "Файл ещё не создан". 
   В случае выполнения задачи статус поменяется на success и выгрузится готовый архив.
3) IMPORT Создание задачи по импорту POST: /public/task/{указываем datasourcesName в который хотим импортировать данные}
   В таблице migration_task созданётся сущность по данной задачи с guid и статусом in progress.
   А также асинхронно начниается миграция данных из архива. 
4) По запросу GET: /public/import/{taskGuid}
   В случае выполнения задачи статус поменяется на success данные импортируются а архив переместится в папку basket, где через неделю будет удалён.