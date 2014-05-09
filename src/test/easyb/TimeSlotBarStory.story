import com.moscona.trading.elements.TimeSlotBar
import com.moscona.trading.formats.deprecated.MarketTree
import com.moscona.trading.streaming.TickStreamRecord
import static com.moscona.test.easyb.TestHelper.*

description "spec for the TimeSlotBar - a speciallized bar that accumulates ticks and has a close() operation"



before_each "scenario", {
  given "a fresh bar", {
    bar = new TimeSlotBar()
    numerics = ["openCents","highCents","lowCents","closeCents","volume"]
  }
  and "the fixture file path", {
    fixture_file = "${fixtures()}/market_tree.csv"
  }
  and "I load a valid market tree file", {
    marketTree = null;
    try {
      marketTree = new MarketTree().load(fixture_file)
    } catch (Exception ex) {
      println "ERROR: "+ex.message
    }
  }
}

scenario "initial state", {
  then "it should have no data available", {
    bar.hasData().shouldBe false
  }
  and "all numeric values should be zero", {
    numerics.each {num ->
      "$num=${bar[num]}".shouldBe "$num=0"
    }
  }
  and "it should not be marked with missing data", {
    bar.isMarkedMissingData().shouldBe false
  }
}

scenario "aggregating the first tick", {
  when "I add one tick", {
    ts = 123
    symbol = "GOOG"
    qty = 1000
    price = 723.45f
    tick = new TickStreamRecord(ts as int,symbol as String,price as float,qty as int,marketTree.symbolToCodeMap,marketTree.codeToSymbolMap)
    bar.add(tick)
  }
  then "it should have data available", {
    bar.hasData().shouldBe true
  }
  and "open,high,low,close should match the value", {
    (numerics-"volume").each { num->
      "$num=${bar[num]}".shouldBe "$num=72345"
    }
  }
  and "volume should be 1000", {
    bar.volume.shouldBe 1000
  }
  and "the tick count should be 1", {
    bar.tickCount.shouldBe 1
  }
  and "the bar should still be considered open", {
    bar.isClosed().shouldBe false
  }
}

scenario "aggregating several ticks", {
  when "I add one tick", {
    ts = 123
    symbol = "GOOG"
    qty = 1000
    price = 723.45f

    ticks = [
      [ts:123, price:700, qty:1000],
      [ts:125, price:702, qty:100],
      [ts:130, price:703, qty:100],
      [ts:124, price:701, qty:200]
    ]
    ticks.each { tick->
      bar.add(new TickStreamRecord(tick.ts as int,symbol as String,tick.price as float,tick.qty as int,marketTree.symbolToCodeMap,marketTree.codeToSymbolMap))
    }
  }
  then "it should have data available", {
    bar.hasData().shouldBe true
  }
  and "open,high,low,close should match the values", {
    bar.openCents.shouldBe 70000
    bar.highCents.shouldBe 70300
    bar.lowCents.shouldBe 70000
    bar.closeCents.shouldBe 70100
  }
  and "volume should be 1400", {
    bar.volume.shouldBe 1400
  }
  and "the tick count should be 4", {
    bar.tickCount.shouldBe 4
  }
  and "the bar should still be considered open", {
    bar.isClosed().shouldBe false
  }
}

scenario "the close() operation", {
  given "that I add one tick", {
    ts = 123
    symbol = "GOOG"
    qty = 1000
    price = 723.45f
    tick = new TickStreamRecord(ts as int,symbol as String,price as float,qty as int,marketTree.symbolToCodeMap,marketTree.codeToSymbolMap)
    bar.add(tick)
  }
  when "I close the bar", {
    bar.close()
  }
  then "it should be closed", {
    bar.isClosed().shouldBe true
  }
  and "I should not be able to add more ticks", {
    3.times(){bar.add(tick)}

    bar.tickCount.shouldBe 1
    bar.volume.shouldBe 1000
  }
}

scenario "marking missing data", {
  when "I mark the bar as missing data", {
    bar.markMissingData()
  }
  then "it should be marked missing data", {
    bar.isMarkedMissingData().shouldBe true
  }
  and "I should be able to add ticks", {
    ts = 123
    symbol = "GOOG"
    qty = 1000
    price = 723.45f
    tick = new TickStreamRecord(ts as int,symbol as String,price as float,qty as int,marketTree.symbolToCodeMap,marketTree.codeToSymbolMap)
    4.times(){ bar.add(tick) }
    bar.volume.shouldBe 4000
    bar.tickCount.shouldBe 4
  }
  and "it should still be marked missing data", {
    bar.isMarkedMissingData().shouldBe true
  }
}