/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See also <a href="https://mkyong.com/java/open-browser-in-java-windows-or-linux/">example1</a>,
 * <a href="https://stackoverflow.com/questions/5226212/how-to-open-the-default-webbrowser-using-java/">example2</a>
 */
public class BrowserHomePageOpener {

    static void open(String url) {
        Logger log = LoggerFactory.getLogger(BrowserHomePageOpener.class);
        log.info("Opening browser with home page \"{}\" ...", url);
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                // this doesn't support showing urls in the form of "page.html#nameLink"
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                // Do a best guess on unix until we get a platform independent way
                // Build a list of browsers to try, in this order.
                String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};
                // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++) {
                    cmd.append(i == 0 ? "" : " || ")
                            .append(browsers[i])
                            .append(" \"")
                            .append(url)
                            .append("\" ");
                }
                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            }
            log.info("Home page \"{}\" opening try finished successfully", url);
        } catch (Exception e) {
            log.info("Can't open home page \"{}\" with browser", url, e);
        }
    }
}
