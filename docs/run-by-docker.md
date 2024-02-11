#### Оглавление
- [Установка на Windows x64](install-on-windows.md)
- [Установка на Windows x86](install-on-windows-by-zip.md)
- [Установка на Mac](install-on-linux.md)
- [Установка на Linux](install-on-linux.md)
- [Запуск в Docker](#запуск-investbook)

### Запуск в Docker контейнере

Способ является альтернативой установке Investbook на [windows](install-on-windows.md), [mac](install-on-linux.md)
и [linux](install-on-linux.md). Запуск в [docker](https://hub.docker.com/r/spaciousteam/investbook) контейнере
отличается более простым процессом обновления.

#### Установка Docker
Перед первым запуском Investbook нужно установить Docker.

##### Установка на Windows
Требуется версия Windows 10 или новее.

1. Скачайте пакет "Docker Desktop" с официального [сайта](https://docs.docker.com/desktop/windows/install/).
2. Установите пакет и запустите "Docker Desktop" из списка приложений.
3. Если "Docker Desktop" предложит обновление по ссылке https://aka.ms/wsl2kernel, можете также скачать и установить его.
4. Если вы работаете не под администратором, то для работы с "Docker Desktop" под текущим пользователем запустите
   командную строку под администратором и выполните
```shell
net localgroup "docker-users" "{User}" /add
```
где вместо "{User}" нужно подставить имя своего пользователя.

##### Установка на MacOS
Требуется версия MacOS 10.15 или новее.

1. Скачайте пакет "Docker Desktop" с официального [сайта](https://docs.docker.com/desktop/mac/install/).
2. Установите пакет и запустите "Docker.app" из списка приложений.
3. Создайте в домашней директории папку "investbook" для хранения данных приложения.

##### Установка на Linux
Пример установки на Ubuntu
```shell
sudo apt install docker.io
sudo usermod --append --groups docker $(whoami)
mkdir ~/investbook
```
#### Запуск Investbook
1. Если используется Windows, запустите Powershell и запустите
   ```shell
   docker run --rm -d -p 2030:2030 -v /c/users/<имя-windows-пользователя>/investbook:/home/cnb/investbook spaciousteam/investbook
   ```
   где вместо `<имя-windows-пользователя>` нужно подставить имя вашего пользователя в Windows.
1. Если используется MacOS или Linux, запустите в терминале
   ```shell
   docker run --rm -d -p 2030:2030 -v ~/investbook:/home/cnb/investbook spaciousteam/investbook
   ```
Будет загружена и запущена последняя версия Investbook. Далее переходите в браузер и открывайте Investbook
по адресу http://localhost:2030

#### Обновление Investbook
Если Investbook запущен, остановите его. Для этого откройте http://localhost:2030 и сверху-справа нажмите кнопку выхода. 
Запустите терминал и удалите текущую версию
```shell
docker rmi spaciousteam/investbook
```
Далее для загрузки и запуска новой версии Investbook выполните команду из предыдущего раздела.

