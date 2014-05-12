import com.moscona.exceptions.InvalidArgumentException
import com.moscona.trading.streaming.TickStreamRecord
import com.moscona.util.TimeHelper
import com.moscona.util.transformation.ByteArrayHelper
import org.apache.commons.lang3.time.DateUtils

import static com.moscona.test.easyb.TestHelper.*


description """IT-43 a logical wrapper for the tick stream record that translates between the high level
representation of the record and the byte array that goes in the buffer. Allows interaction with buffer
records like a normal class.
"""

before_each "scenario", {
  symbols = ["GOOG","IBM","XOM"]

  symbolToCodeMap = new HashMap<String,Integer>()
  codeToSymbolMap = new HashMap<Integer,String>()

  symbols.eachWithIndex{sym,i ->
    symbolToCodeMap[sym] = i
    codeToSymbolMap[i] = sym
  }

  TimeHelper.switchToNormalMode()

  tzOffset =  TimeZone.default.rawOffset - TimeZone.getTimeZone("US/Eastern").rawOffset

  cal = Calendar.instance
  d = [cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)]
  today = Calendar.instance
  today.set(*d,9,30,0)
  def oldTs = today.timeInMillis
  today.set(Calendar.YEAR, 2010)
  yearsOffset = oldTs - today.timeInMillis

  startOfTradingDayTs = today.time.time + tzOffset

  result = null;

}

//now = new Date().time
//cal = Calendar.instance
//d = [cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)]
//today = Calendar.instance
//today.set(*d,0,0,0)
//start = today.time.time
//[(now-start)/3600000,now>start, today.time]

//est = TimeZone.getTimeZone("US/Eastern")
//here = TimeZone.getDefault()
//["local":here,"EST":est,"offset":(here.rawOffset - est.rawOffset)/3600000]

scenario "creating a record", {
  given "constructor arguments", {
    params = [
      "ts": startOfTradingDayTs + 3600123, // one hour and 123 ms after start of trading day
      "symbol": "GOOG",
      "price": 789.89f,
      "qty": 1000
    ]
  }
  then "I should be able to create a record using the arguments", {
    record = new TickStreamRecord(params.ts, params.symbol, params.price, params.qty, symbolToCodeMap, codeToSymbolMap)
  }
}

scenario "translating a symbol into a byte[2]", {
  when "coverting IBM to bytes", {
    bytes = TickStreamRecord.symbolToBytes("IBM", symbolToCodeMap)
  }
  then "I should get [0,1]", {
    expected = new byte[2]
    expected[0] = 0;
    expected[1] = 1;

    bytes.shouldBe expected
  }
}

scenario "translating a byte[2] into a symbol", {
  when "converting [0,2] to a symbol", {
    bytes = new byte[2]
    bytes[0] = 0
    bytes[1] = 2
  }
  then "I should get XOM", {
    TickStreamRecord.bytesToSymbol(bytes, codeToSymbolMap).shouldBe "XOM"
  }
}

scenario "translating a small integer into a byte[2]", {
  given "an integer that fits in two bytes: 32767==2^15-1", { result = ByteArrayHelper.intToBytes(32767,2) }
  then "I should be able to cleanly convert it", {
    "${result}".shouldBe "[127, -1]"
  }
}

scenario "translating a byte[2] into a small integer", {
  given "a byte array [127,-1]", {
    arr = new byte[2]
    arr[0] = 127
    arr[1] = -1
  }
  when "converting it to an int", { result = ByteArrayHelper.byteArrayToInt(arr) }
  then "I should get 32767", { result.shouldBe 32767 }
}

scenario "illegal values for a symbol key (out of boundaries)", {
  given "a code that does not exist in the system", { code = 15 }
  then "when I try to convert it to a symbol I should get an exception", {
    ensureThrows(InvalidArgumentException) {
      TickStreamRecord.bytesToSymbol(15, codeToSymbolMap)
    }
  }
}



description "internal server timestamps are always expressed relative to last midnight US/Eastern"

scenario "translating time to integer", {
  given "a calendar object in the America/Los_Angeles time zone and today's date", {
    cal = Calendar.instance
    tz = TimeZone.getTimeZone("America/Los_Angeles")
    cal.timeZone = tz
  }
  and "we force midnight recalculation", {
    //noinspection GroovyAccessibility
    TimeHelper.recalculateLastMidnight()
  }
  and "the time is set to 10am to the millisecond", {
    d = [cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)]
    cal.set(*d, 10,0,0)
    cal.set(Calendar.MILLISECOND,0)
  }
  when "I convert it to the server internal timestamp (int)", {
    result = TimeHelper.convertToInternalTs(cal)
  }
  then "I should get the value 46800000, which is the number of milliseconds from midnight in the time zone US/Eastern", {
    [46800000, -39600000].contains(result).shouldBe true // the negative value occurs when the test is ran after 9pm pst
  }
}

scenario "translating an integer back into time", {
  given "the internal server timestamp 47880000", { ts = 47880000 }
  and "we force midnight recalculation", {
    //noinspection GroovyAccessibility
    TimeHelper.recalculateLastMidnight()
  }
  when "it is converted to a real calendar timestamp", {
    result = TimeHelper.convertToCalendar(ts as int)

  }
  then "it should result in exactly 1:18:00pm today (to the millisecond)", {
    result.get(Calendar.HOUR_OF_DAY).shouldBe 13
    result.get(Calendar.MINUTE).shouldBe 18
    result.get(Calendar.SECOND).shouldBe 0
    result.get(Calendar.MILLISECOND).shouldBe 0

    today = Calendar.instance
    result.get(Calendar.YEAR).shouldBe today.get(Calendar.YEAR)
    def expected = [today.get(Calendar.DAY_OF_YEAR), today.get(Calendar.DAY_OF_YEAR)+1]
    expected.contains(result.get(Calendar.DAY_OF_YEAR)).shouldBe true // in the scenario of running after 9pm you get the following day
  }
  and "the time zone of the result should be US/Eastern",{ result.timeZone.ID.shouldBe "US/Eastern" }
}

scenario "getting the current time in EST", {
  given "a default Calendar instance", { now = Calendar.instance }
  when "I convert it to an internal timestamp", { ts = TimeHelper.convertToInternalTs(now) }
  and "then convert it back", { result = TimeHelper.convertToCalendar(ts) }
  then "I should get back a calendar object in US/Eastern time", { result.timeZone.ID.shouldBe "US/Eastern" }
  and "the two calendars should agree on their meaning in millis", { result.timeInMillis.shouldBe now.timeInMillis}
}

scenario "translating byte[4] to a timestamp string with sufficient resolution", {
  given "a timestamp value of 47880123", { ts = 47880123 }
  and "a conversion of it to a byte[4]", { bytes = ByteArrayHelper.intToBytes(ts,4) }
  and "we force midnight recalculation", {
    //noinspection GroovyAccessibility
    TimeHelper.recalculateLastMidnight()
  }
  when "I convert it to a string using the byte[4] signature", { result = ByteArrayHelper.convertToString(bytes as byte[]) }
  then "I should get a value that includes the miliseconds in a mysql string form", {
    cal = Calendar.instance
    year = cal.get(Calendar.YEAR)
    month = cal.get(Calendar.MONTH)+1
    day = cal.get(Calendar.DAY_OF_MONTH)

    def expected = "${year}-${month.toString().padLeft(2, "0")}-${day.toString().padLeft(2, "0")} 13:18:00.123 EDT"
    result.replaceAll(" EST"," EDT").shouldBe expected
  }
}


scenario "translating integer to a byte[4]", {
  given "the value 2147481647", { value = 2147481647 }
  when "I convert it to byte[4]", { result = ByteArrayHelper.intToBytes(value,4) }
  then "I should get the correct byte array representation", { "$result".shouldBe "[127, -1, -8, 47]" }
}

scenario "translating byte[4] to integer", {
  given "the byte array [127, -1, -8, 47]", { bytes = ([127, -1, -8, 47] as byte[]) }
  when "I convert it to an integer", { result = ByteArrayHelper.byteArrayToInt(bytes) }
  then "I should get 2147481647", { result.shouldBe 2147481647 }
}

scenario "compatibility of int to byte[4] with java.nio.ByteBuffer", {
  given "the integer 123456", { num = 123456 }
  when "I convert it using the ByeArrayHelper", { result = ByteArrayHelper.intToBytes(num,4) as List }
  then "I should get the same result as when I use java.nio.ByteBuffer", {
    buf = java.nio.ByteBuffer.allocate(4)
    buf.putInt(num)
    expected = buf.array() as List
    result.shouldBe expected
  }
}

scenario "translating price into a byte[4]", {
  given "the price 527.45", { price = 527.45f }
  when "I convert it to a byte[3]", { result = TickStreamRecord.priceToBytes(price) }
  then "I should get the correct byte array", { "$result".shouldBe "[0, 0, -50, 9]" }
}

scenario "translating byte[4] into price", {
  given "the byte array [0, 0, -50, 9]", { bytes = ([0, 0, -50, 9] as byte[]) }
  when "I convert it to a price", { result = TickStreamRecord.bytesToPrice(bytes) }
  then "I should get 527.45", { result.shouldBe 527.45f }
}

scenario "translating a whole com.intellitrade.TickStreamRecord into a byte[RECORD_LENGTH]", {
  given "A tick stream record", {
    cal = Calendar.instance
    ts = cal.timeInMillis // whatever
    symbol = "GOOG"
    price = 723.99f
    qty = 1000
    record = new TickStreamRecord(ts, symbol, price, qty, symbolToCodeMap, codeToSymbolMap)
  }
  when "I convert it to bytes", { result = record.toBytes() }
  then "I should get the correct byte array value for the entire record", {
    symbolBytes = ByteArrayHelper.intToBytes(symbolToCodeMap[symbol], 2) as List
    priceBytes = ByteArrayHelper.floatToBytes(price,4,100) as List
    qtyBytes = ByteArrayHelper.intToBytes(qty,4) as List

    tsBytes = ByteArrayHelper.intToBytes(TimeHelper.convertToInternalTs(cal),4) as List
    insBytes = [0,0,0,0]

    // in the following the second timestamp is the buffer entry timestamp, which we will ignore in this step
    expected = (tsBytes + symbolBytes + priceBytes + qtyBytes + insBytes)
    result = result as List
    result.shouldBe expected
  }
}

scenario "translating a byte[RECORD_LENGTH] into a com.intellitrade.TickStreamRecord", {
  given "a byte representation of a record", {
    bytes = [
      3, -88, 69, 81,  // transaction timestamp
      0, 0,            // symbol code
      0, 1, 26, -49,   // price*100 as int
      0, 0, 3, -24,    // quantity
      3, -88, 69, 82   // insertion timestamp
    ] as byte[]
  }
  when "I set it as the value of the record", { record = new TickStreamRecord(bytes, symbolToCodeMap, codeToSymbolMap) }
  then "I should get the correct record values", {
    record.transactionTimestamp.shouldBe 61359441
    record.symbol.shouldBe "GOOG"
    (record.price - 723.99).shouldBeLessThan 0.005
    record.quantity.shouldBe 1000
    record.insertionTimestamp.shouldBe record.transactionTimestamp + 1
  }
}

scenario "timestamping insertion timestamps into the record", {
  given "a record with no timestamp", {
    record = new TickStreamRecord(Calendar.instance.timeInMillis, "XOM", 32.4f, 10000, symbolToCodeMap, codeToSymbolMap)
  }
  when "I set the timestamp", { record.setInsertionTimestamp() }
  then "I should get a record with an internal timestamp", {
    ts = record.insertionTimestamp
    cal = TimeHelper.convertToCalendar(ts)
    ms = cal.timeInMillis
    now = Calendar.getInstance(TimeHelper.INTERNAL_TIMEZONE).timeInMillis
    (now - ms).shouldBeLessThan 10
  }
}



description "Misc. tests"

scenario "Replacing the internal byte buffer (mostly for testability)", {
  given "a byte representation of a record", {
    bytes = [
      3, -88, 69, 81,  // transaction timestamp
      0, 0,            // symbol code
      0, 1, 26, -49,   // price*100 as int
      0, 0, 3, -24,    // quantity
      3, -88, 69, 82   // insertion timestamp
    ] as byte[]
  }
  and "a replacement value", {
    replacement = [
      1,1,1,1,  // transaction timestamp
      2,2,            // symbol code
      3,3,3,3,   // price*100 as int
      4,4,4,4,    // quantity
      5,5,5,5   // insertion timestamp
    ] as byte[]
  }
  when "I set it as the value of the record", { record = new TickStreamRecord(bytes, symbolToCodeMap, codeToSymbolMap) }
  and "replace the values with a new byte array", { record.replaceBytes(replacement) }
  then "the record should return the new byte array the next time", { record.toBytes().shouldBe replacement }
}

scenario "Comparing two TickStreamRecord objects while ignoring the insertion timestamp", {
  given "two records differing only by the timestamp", {
    bytes = [
      3, -88, 69, 81,  // transaction timestamp
      0, 0,            // symbol code
      0, 1, 26, -49,   // price*100 as int
      0, 0, 3, -24,    // quantity
      3, -88, 69, 82   // insertion timestamp
    ]
    r1 = new TickStreamRecord(bytes.clone() as byte[], symbolToCodeMap, codeToSymbolMap)
    bytes[-1] = 81 // replace a byte in the insertion timestamp
    r2 = new TickStreamRecord(bytes.clone() as byte[], symbolToCodeMap, codeToSymbolMap)
  }
  and "a third record differing by the transaction timestamp", {
    bytes[0] = 2  // replace a byte in the transaction timestamp
    r3 = new TickStreamRecord(bytes.clone() as byte[], symbolToCodeMap, codeToSymbolMap)
  }
  then "the two first records should be considered equivalent using equalsWithoutInsertionTs()", {
    r1.equalsWithoutInsertionTs(r2).shouldBe true
  }
  and "the first and third should be considered different", {
    r1.equalsWithoutInsertionTs(r3).shouldBe false
  }
}

scenario "Converting a TickStreamRecord to a string", {
  given "a record with a known value", {
    bytes = [
      3, -88, 69, 81,  // transaction timestamp
      0, 0,            // symbol code
      0, 1, 26, -49,   // price*100 as int
      0, 0, 3, -24,    // quantity
      3, -88, 69, 82   // insertion timestamp
    ] as byte[]
    record = new TickStreamRecord(bytes, symbolToCodeMap, codeToSymbolMap)
  }
  then "the toString() method should show the correct values", {
    record.toString().shouldBe "Symbol=GOOG price=723.99 quantity=1000 trans.ts=61359441 insertion.ts=61359442"
  }
}