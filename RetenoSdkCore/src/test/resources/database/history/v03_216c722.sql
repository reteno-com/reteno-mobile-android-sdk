-- schema of database version 3 at commit 216c722
CREATE TABLE IF NOT EXISTS AppInbox(messageId TEXT PRIMARY KEY, deviceId TEXT, status TEXT, time TEXT);
CREATE TABLE IF NOT EXISTS Device(row_id INTEGER PRIMARY KEY, timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deviceId TEXT NOT NULL, externalUserId TEXT, pushToken TEXT, pushSubscribed TEXT, category TEXT NOT NULL, osType TEXT NOT NULL, osVersion TEXT, deviceModel TEXT, appVersion TEXT, languageCode TEXT, timeZone TEXT, advertisingId TEXT);
CREATE TABLE IF NOT EXISTS Events(events_id INTEGER PRIMARY KEY, deviceId TEXT, externalUserId TEXT);
CREATE TABLE IF NOT EXISTS Event(events_id INTEGER NOT NULL, row_id INTEGER PRIMARY KEY, eventTypeKey TEXT, occurred TIMESTAMP, params TEXT, FOREIGN KEY (events_id) REFERENCES Events (events_id));
CREATE TABLE IF NOT EXISTS Interaction(row_id INTEGER PRIMARY KEY, timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, interactionId TEXT, status TEXT, time TEXT, token TEXT);
CREATE TABLE IF NOT EXISTS RecomEvents(recomVariantId TEXT PRIMARY KEY NOT NULL);
CREATE TABLE IF NOT EXISTS RecomEvent(recomVariantId TEXT NOT NULL, rowId INTEGER PRIMARY KEY, productId TEXT, occurred TIMESTAMP, eventType TEXT, FOREIGN KEY (recomVariantId) REFERENCES RecomEvents (recomVariantId));
CREATE TABLE IF NOT EXISTS User(user_row_id INTEGER PRIMARY KEY, timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deviceId TEXT, externalUserId TEXT, subscriptionKeys TEXT, groupNamesInclude TEXT, groupNamesExclude TEXT);
CREATE TABLE IF NOT EXISTS UserAttributes(user_row_id INTEGER NOT NULL, phone TEXT, email TEXT, firstName TEXT, lastName TEXT, languageCode TEXT, timeZone TEXT, fields TEXT, FOREIGN KEY (user_row_id) REFERENCES User (user_row_id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS UserAddress(user_row_id INTEGER NOT NULL, region TEXT, town TEXT, address TEXT, postcode TEXT, FOREIGN KEY (user_row_id) REFERENCES User (user_row_id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS WrappedLink(row_id INTEGER PRIMARY KEY, timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, url TEXT);
