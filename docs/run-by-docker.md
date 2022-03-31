### Запуск в Docker контейнере

Способ является альтернативой установке Investbook на [windows](install-on-windows.md), [mac](install-on-linux.md)
и [linux](install-on-linux.md). Способ отличается более простым процессом обновления.

#### Установка Docker
Перед первым запуском Investbook нужно установить docker.

##### Установка на Windows
Требуется версия Windows 10 или новее.

1. Скачайте пакет Docker Desktop с официального [сайта](https://docs.docker.com/desktop/windows/install/).
2. Установите пакет и запустите Docker Desktop из списка приложений.
3. Если Docker Desktop предложит обновление по ссылке https://aka.ms/wsl2kernel, можете также скачать и установить его.

##### Установка на MacOS
Требуется версия MacOS 10.15 или новее.

1. Скачайте пакет Docker Desktop с официального [сайта](https://docs.docker.com/desktop/mac/install/).
2. Установите пакет и запустите Docker.app из списка приложений.
3. Создайте в домашней директории папку "investbook" для хранения данных приложения.

##### Установка на Linux
Пример установки на Ubuntu
```shell
sudo apt install docker.io
sudo usermod --append --groups docker $(whoami)
mkdir ~/investbook
```
#### Запуск Investbook
1. Если используется Windows, запустите powershell и запустите
   ```shell
   docker run --rm -d -p 80:80 -v /c/users/<имя-windows-пользователя>/investbook:/home/cnb/investbook -e LANG=C.UTF-8 spaciousteam/investbook
   ```
   где вместо `<имя-windows-пользователя>` нужно подставить имя вашего пользователя в Windows.
1. Если используется MaсOS или Linux, запустите в терминале
   ```shell
   docker run --rm -d -p 80:80 -v ~/investbook:/home/cnb/investbook -e LANG=C.UTF-8 spaciousteam/investbook
   ```
Будет загружена и запущена последняя версия Investbook. Далее переходите в браузер и открывайте Investbook
по адресу http://localhost

#### Обновление Investbook
Если Investbook запущен, остановите его. Для этого откройте http://localhost и сверху-справа нажмите кнопку выхода. 
Запустите терминал и удалите текущую версию
```shell
docker rmi spaciousteam/investbook:latest
```
Далее для загрузки и запуска новой версии Investbook выполните команду из предыдущего раздела.

