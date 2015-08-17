/*
 * Copyright (c) 2015. Arnon Moscona
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.moscona.exceptions.InvalidStateException
import com.moscona.trading.formats.deprecated.MarketTree
import static com.moscona.test.easyb.TestHelper.*
import static com.moscona.test.easyb.MarketTreeTestHelper.*

description "Behavior of the internal representation of a market tree in the server"

before "all scenarios", {
  given "the fixture file path", {
    fixture_file = "${fixtures()}/market_tree.csv"
  }
  and "I load a valid market tree file", {
    valid_tree = null;
    try {
      valid_tree = loadMarketTreeFixture("market_tree_with_index.csv")
    } catch (Exception ex) {
      println "ERROR: "+ex.message
    }
  }
  and "a backup of the maps", {
    symbolToCodeBackup = [:]
    symbolToCodeBackup.putAll(valid_tree.symbolToCodeMap)
    codeToSymbolBackup = [:]
    codeToSymbolBackup.putAll(valid_tree.codeToSymbolMap)
  }
  and "a backup of the tree", {
    treeBackup = [:]
    symbolToCodeBackup.each{ k,v -> treeBackup[k] = valid_tree.get(k).clone() }
  }
}

before_each "scenario", {
  given "the fixture file path", {
    fixture_file = "${fixtures()}/market_tree.csv"
  }
  and "a restored backup of the maps", {
    valid_tree.symbolToCodeMap.clear()
    valid_tree.codeToSymbolMap.clear()
    valid_tree.symbolToCodeMap.putAll(symbolToCodeBackup)
    valid_tree.codeToSymbolMap.putAll(codeToSymbolBackup)
  }
  and "a restored backup of the tree", {
    treeBackup.each { k,v -> valid_tree.get(k).copy(treeBackup[k])}
  }
}



scenario "validating that the fixture is there", {
  then "the fixture file should exist", {
    new File(fixture_file).canRead().shouldBe true
  }
}

scenario "loading a market tree from file", {
  given "an empty market tree object", { tree = new MarketTree() }
  then "there should not be any exceptions when I load it", {
    tree.load(fixture_file)
  }
  and "the market tree object should be populated", {
    tree.size().shouldBe 586
  }
  and "it should have two index items", {
    tree.getIndexSymbols().size().shouldBe 2
  }
  and "it should have the correct amount of stocks", {
    tree.getStockSymbols().size().shouldBe 502
  }
  and "it should have the correct amount of industries", {
    tree.getIndustrySymbols().size().shouldBe 82
  }
  and "it should have the correct amount of quotables", {
    tree.getQuotableSymbols().size().shouldBe 504
  }
}



description "State validation"


scenario "validation", {
  then "the isValid() method should say true", { valid_tree.isValid().shouldBe true }
}

scenario "validation2", {
  then "the listStateProblems() method should return an empty string", { valid_tree.listStateProblems().shouldBe "" }
}

scenario "validating structure sizes", {
  when "I remove an entry from the symbolToCode map", {
    valid_tree.symbolToCodeMap.remove("GOOG")
  }
  then "the validation should fail", {
    valid_tree.isValid().shouldBe false
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

scenario "validating relational integrity", {
  when "I change the parent of GOOG to something that does not exist", {
    node = valid_tree.get("GOOG")
    node.parent = "something fictional"
  }
  then "the validation should fail", {
    valid_tree.isValid().shouldBe false
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

scenario "validating weights", {
  when "I change the weight og GOOG so that the total does not add up to 1000", {
    node = valid_tree.get("GOOG")
    node.weight += 10
  }
  then "the validation should fail", {
    valid_tree.isValid().shouldBe false
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

scenario "validating 52 week range", {
  when "I change the 52-week low to be higher than the 52-week high", {
    valid_tree.get("WFC").yearLow = valid_tree.get("WFC").yearHigh+0.01
  }
  then "the validation should fail", {
    valid_tree.isValid().shouldBe false
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

scenario "validating price against 52 week range", {
  when "I put the last price outside the 52-week range", {
    valid_tree.get("BAC").price = valid_tree.get("BAC").yearHigh+0.01
  }
  then "the validation should fail", {
    valid_tree.isValid().shouldBe false
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

scenario "validating previous price against 52 week range", {
  when "I put the previous to last price outside the 52-week range", {
    valid_tree.get("T").prevClose = valid_tree.get("T").yearLow-0.01
  }
  then "the validation should fail", {
    ensure(valid_tree.listStateProblems()) {
      contains("price is lower than the 52 week low")
    }
  }
  and "the problem listing should contain some explanation", {
    valid_tree.listStateProblems().size().shouldBeGreaterThan 10
  }
}

// todo missing functionality in MarketTree

description "Industry aggregates IT-47"
description "Traversal and descendant listings IT-48"

scenario "Writing a market tree file IT-49", {
  given "a second market tree variable", {
    newTree = new MarketTree()
  }
  and "an exception holder", {
    exception = null
  }
  and "a raw text variable", {
    text = null
  }
  when "I change values in the market tree, write the market tree file, and read it back", {
    try {
      printExceptions {
        valid_tree.get("IBM").setPrice(100.0f)
        valid_tree.write(fixture_file)
        newTree.load(fixture_file)
        text = new File(fixture_file as String).text
      }
    }
    catch (Throwable e) {
      exception = e
    }
  }
  then "no exceptions are thrown", {
    ensure(exception){isNull()}
  }
  and "the new market three reflects the changes", {
    ensureFloatClose(newTree.get("IBM").price, 100.00,0.001)
  }
  and "the new market tree file lines are delimted by CRLF", {
    ((text =~ /(.*(\r\n))+/)  as boolean).shouldBe true
  }
  and "there are no quotes in the file", {
    ((text =~ /.*".*/)  as boolean).shouldBe false
  }
}

scenario "testing for staleness", {
  given "two calendar objects", {
    lastModifiedToday = Calendar.instance
    lastModifiedToday.set(2010,5,11,15,0,0)
    lastModifiedYesterday = Calendar.instance
    lastModifiedYesterday.set(2010,5,10,15,0,0)
    now = Calendar.instance
    now.set(2010,5,11,17,0,0)
  }
  then "comparing now and last (today) should yield stale", {
    valid_tree.isStale(lastModifiedToday,now).shouldBe true
  }
  and "comparing now and last (yesterday) should yield stale", {
    valid_tree.isStale(lastModifiedYesterday,now).shouldBe true
  }
  and "comparing today to today with last updated after market close should not be stale", {
    lastModifiedToday.set(2010,5,11,16,1,0)
    valid_tree.isStale(lastModifiedToday,now).shouldBe false
  }
  and "comparing to today with both before market close should not be stale", {
    lastModifiedToday.set(2010,5,11,15,0,0)
    now.set(2010,5,11,15,30,0)
    valid_tree.isStale(lastModifiedToday,now).shouldBe false
  }
  and "comparing with yesterday, but last update is after market close housld not be stale", {
    lastModifiedYesterday.set(2010,5,10,19,0,0)
    valid_tree.isStale(lastModifiedYesterday,now).shouldBe false
  }
  and "comparing to over 24 hours ago, even if the update was after market close should be stale", {
    lastModifiedYesterday.set(2010,5,9,19,0,0)
    valid_tree.isStale(lastModifiedYesterday,now).shouldBe true
  }
}


scenario "parent-child relationships", {
  then "basic materials shouls be a child of SP500", {
    valid_tree.get("Data Storage").isChildOf(valid_tree.get("Technology")).shouldBe true
  }
  and "INTC should be a child of semiconductors", {
    valid_tree.get("INTC").isChildOf(valid_tree.get("Semiconductors")).shouldBe true
  }
  and "INTC should be a child of technology", {
    valid_tree.get("INTC").isChildOf(valid_tree.get("Technology")).shouldBe true
  }
  and "INTC should not be a child of Data Storage", {
    valid_tree.get("INTC").isChildOf(valid_tree.get("Data Storage")).shouldBe false
  }
  and "The depth of Technology should be 0", {
    valid_tree.get("Technology").depth().shouldBe 0
  }
  and "The depth of Semiconductors should be 1", {
    valid_tree.get("Semiconductors").depth().shouldBe 1
  }
  and "The depth of INTC should be 2", {
    valid_tree.get("INTC").depth().shouldBe 2
  }
}

scenario "TreeEntry sorting", {
  then "Autos and Parts should be > Consumer Cyclicals", {
    valid_tree.get("Autos and Parts").shouldBeGreaterThan(valid_tree.get("Consumer Cyclicals"))
    valid_tree.get("Consumer Cyclicals").shouldBeLessThan(valid_tree.get("Autos and Parts"))
  }
  and "cpmparing the same two should yield -1", {
    ensure(valid_tree.get("Autos and Parts").compareTo(valid_tree.get("Consumer Cyclicals"))){equals(-1)}
  }
}

// Additions due to adding required quotables ======================================================

scenario "When loading a market tree that is associated with a master tree with reuired nodes, the market tree should not validate if it's missing required nodes", {
  given "a master tree with required entries", {
    printExceptions {
      fixtureFileName = "master_tree_with_required.csv"
      masterTree = loadMasterTreeFixture(fixtureFileName)
    }
  }
  and "a market tree with a missing required row", {
    marketTree = loadMarketTreeFixture("market_tree_with_index_missing_one.csv")
  }
  when "I associated the market tree with the master tree", {
    marketTree.setMaster(masterTree)
  }
  and "an exception holder", {
    exception = null
  }
  then "validate() validate should fail", {
    ensureThrows(InvalidStateException) {
//      printExceptions {
        marketTree.validate()
//      }
    }
  }
}

scenario "checking whether a market tree conforms to an associated masster tree: no changes" , {
  when "I associate the masket tree with the market tree", {
    marketTree.setMaster(masterTree)
  }
  then "the confomance test should pass", {
    marketTree.conformsToMaster().shouldBe true
  }
}

scenario "checking whether a market tree conforms to an associated masster tree: quotable list changed" , {
  given "a master tree with a different quotable list", {
    masterTree.get("BAC").status = "disabled"
  }
  when "I associate it with the market tree", {
    marketTree.setMaster(masterTree)
  }
  then "the confomance test should fail", {
    marketTree.conformsToMaster().shouldBe false
  }
}

scenario "checking whether a market tree conforms to an associated masster tree: tree structure changed" , {
  given "a master tree with a different tree structure", {
    masterTree.get("Gold Mining").parent = "Consumer Staples"
  }
  when "I associate it with the market tree", {
    marketTree.setMaster(masterTree)
  }
  then "the confomance test should fail", {
    marketTree.conformsToMaster().shouldBe false
  }
}
