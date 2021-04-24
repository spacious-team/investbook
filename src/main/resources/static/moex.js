/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

async function uploadSecurities(inputIdWithQueryString, securitiesDatalistElementId, callingElement) {
    let queryString = document.getElementById(inputIdWithQueryString).value
    console.log("Поиск бумаги по: ", queryString)
    callingElement.classList.add("blink")
    await findAndUploadSecurities(queryString, securitiesDatalistElementId)
    callingElement.classList.remove("blink")
}

/**
 * @param queryString part of security name
 * @param securitiesDatalistElementId datalist html element for append securities
 */
async function findAndUploadSecurities(queryString, securitiesDatalistElementId) {
    let securitiesElement = document.getElementById(securitiesDatalistElementId)

    let responce = await fetch('http://iss.moex.com/iss/securities.json?' +
        'iss.meta=off&securities.columns=name,isin&q=' + queryString)
    if (responce.ok) {
        let securities = (await responce.json()).securities.data
        if (securities.length > 0) {
            addToDatalist(securities, securitiesElement);
        }
    }
}

/**
 * @param securitiesDatalistElementId datalist html element for append securities
 */
async function uploadAllSecurities(securitiesDatalistElementId) {
    // http://iss.moex.com/iss/index
    // securitygroups
    let securityGroups = ['stock_shares', 'stock_bonds', 'stock_foreign_shares', 'stock_eurobond',
        'stock_etf', 'stock_ppif', 'stock_qnv']

    let securitiesElement = document.getElementById(securitiesDatalistElementId)

    for (let securityGroup of securityGroups) {
        for (let start = 0; start < 100000; start += 100) {
            let responce = await fetch('http://iss.moex.com/iss/securities.json?' +
                'iss.meta=off&securities.columns=name,isin&group_by=group&group_by_filter=' + securityGroup +
                '&start=' + start)
            if (responce.ok) {
                let securities = (await responce.json()).securities.data
                if (securities.length === 0) {
                    break
                } else {
                    addToDatalist(securities, securitiesElement);
                }
            }
        }
    }
}

/**
 * @param securities list of arrays [0 - name, 1 - isin]
 * @param securitiesElement datalist html element for append securities
 */
function addToDatalist(securities, securitiesElement) {
    for (let security of securities) {
        let name = security[0] // &securities.columns=name,isin
        let isin = security[1] // &securities.columns=name,isin
        if (name !== null && isin !== null) {
            let securityDescription = name + ' (' + isin + ')'
            var newOptionElement = document.createElement("option");
            newOptionElement.textContent = securityDescription;
            securitiesElement.appendChild(newOptionElement);
            console.log('Найдена бумага: ', securityDescription)
        }
    }
}
