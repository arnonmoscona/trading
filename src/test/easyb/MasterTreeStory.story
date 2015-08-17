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

import com.moscona.trading.formats.deprecated.MarketTree
import com.moscona.trading.formats.deprecated.MasterTree
import static com.moscona.test.easyb.TestHelper.*
import static com.moscona.test.easyb.MarketTreeTestHelper.*

description "tests the functionality of the master tree, which is used in market tree generation"


before_each "scenario", {
  given "a copy of the master tree", {
    masterTree = loadMasterTreeFixture("master_tree_with_index.csv")
  }
  and "a configuration pointing to the master tree path", {
    treePath = "${fixtures()}/master_tree.csv"
  }
}

scenario "loading the tree", {
  given "and exception holder", { exception = null }
  when "I load the tree", {
    try {
      printExceptions {
        masterTree = new MasterTree().load(treePath)
      }
    } catch (Throwable ex) {
      exception = ex
    }
  }
  then "no exception should be raised", {
    exception.shouldBe null
  }
  and "the market tree should not be null", {
    masterTree.shouldNotBe null
  }
  and "it should have the right number of entries", {
    masterTree.size().shouldBe 586
  }
  and "index nodes should be quotable", {
    masterTree.get("INDU.X").isQuotable().shouldBe true
  }
  and "stock nodes should be quotable", {
    masterTree.get("IBM").isQuotable().shouldBe true
  }
  and "industry nodes should not be quotable", {
    masterTree.get("Technology").isQuotable().shouldBe false
  }
  and "the disabled count should be 1", {
    masterTree.disabledCount.shouldBe 1
  }
}

scenario "behavior as iterator", {
  given "a type counter", {
    counts = [:]
    counts[MarketTree.TreeEntry.STOCK] = 0
    counts[MarketTree.TreeEntry.INDUSTRY] = 0
    counts[MarketTree.TreeEntry.INDEX] = 0
  }
  when "I iterate over all entries", {
    printExceptions {
      for (e in (masterTree as Iterable<MasterTree.TreeEntry>)) {
        counts[e.getType()] +=1
      }
    }
  }
  then "I should get the right count for stocks", {
    counts[MarketTree.TreeEntry.INDUSTRY].shouldBe 82
  }
  and "I should get the right count for stocks", {
    counts[MarketTree.TreeEntry.STOCK].shouldBe (masterTree.size() - 84)
  }
  and "I should get the right count for indices", {
    counts[MarketTree.TreeEntry.INDEX].shouldBe 2
  }
}


scenario "updating the file" , {
  given "a temporary output path", {
    tempPath = tempFile().canonicalPath
    clearTmpDir()
  }
  and "an exception holder", { exception = null }
  when "I write the file", {
    printExceptions {
      try {
        masterTree.write(tempPath)
      }
      catch (Throwable e) {
        exception = e
      }
    }
  }
  then "I should get no exceptions", {
    ensure(exception) {
      isNull()
    }
  }
  and "the file should exist", {
    new File(tempPath as String).exists().shouldBe true
  }
  and "I should get an equivalent master tree", {
    printExceptions {
      newTree = new MasterTree().load(tempPath)
      newTree.size().shouldBe 586
    }
    masterTree.equals(newTree).shouldBe true
    newTree.disabledCount.shouldBe 1
  }
}

scenario "loading a tree with various status changes", {
  given "a master tree file that has rows with all status types", {
    fileName = "master_tree_status_test.csv"
  }
  and "an exception holder", {
    exception = null
  }
  when "I load it and validate it", {
    try {
      printExceptions {
        masterTree = loadMasterTreeFixture(fileName)
        masterTree.validate()
      }
    }
    catch(Throwable e) {
      exception = e
    }
  }
  then "there should be no exceptions", {
    exception.shouldBe null
  }
  and "NEWTHING should be new", {
    masterTree.get("NEWTHING").status.shouldBe "new"
  }
  and "SSP should be in error", {
    masterTree.get("SSP").status.shouldBe "error"
  }
  and "CROX should be stale", {
    masterTree.get("CROX").status.shouldBe "stale"
  }
  and "TTWO should be deleted", {
    masterTree.get("TTWO").status.shouldBe "deleted"
  }
  and "the iterator should skip TTWO", {
    found = false
    for (e in (masterTree as Iterable<MasterTree.TreeEntry>)) {
      if (e.name == "TTWO") {
        found = true
      }
    }
    found.shouldBe false
  }
  and "when I write it back then TTWO should not be there any more", {
    printExceptions {
      masterTree.write(tempPath)
      new MasterTree().load(tempPath).get("TTWO").shouldBe null
    }
  }
}

// Additions due to adding required quotables ======================================================

scenario "Loading an old master tree should result in quotables being considered not required", {
  then "the SPX.XO node should not be required", {
    masterTree.get("SPX.XO").isRequired().shouldBe false
  }
}

scenario "Loading a new master tree with some required indexes and stocks should not fail", {
  given "a master tree with required lines" ,{
    fixtureFileName = "master_tree_with_required.csv"
  }
  when "I load the tree", {
    try {
      printExceptions {
        masterTree = loadMasterTreeFixture(fixtureFileName)
      }
    } catch (Throwable ex) {
      exception = ex
    }
  }
  then "no exception should be raised", {
    exception.shouldBe null
  }
}

scenario "Loading a master tree with required SPO.XO should result in that node being required", {
  when "I load a master tree with required nodes", {
    printExceptions {
      fixtureFileName = "master_tree_with_required.csv"
      masterTree = loadMasterTreeFixture(fixtureFileName)
    }
  }
  then "the SPO.XO node should be required", {
    masterTree.get("SPX.XO").isRequired().shouldBe true
  }
  and "the BAC node should be required", {
    masterTree.get("BAC").isRequired().shouldBe true
  }
  and "the WFC node should not be required", {
    masterTree.get("WFC").isRequired().shouldBe false
  }
}

scenario "updating a master tree on disk should not changed required status of required nodes", {
  given "I load a master tree with required nodes", {
    printExceptions {
      fixtureFileName = "master_tree_with_required.csv"
      masterTree = loadMasterTreeFixture(fixtureFileName)
    }
  }
  and "a temporary output path", {
    tempPath = tempFile().canonicalPath
    clearTmpDir()
  }
  and "I change INDU.X to required", {
    masterTree.get("INDU.X").setIsRequired(true);
  }
  and "I save it", {
    masterTree.write(tempPath)
  }
  and "reload it", {
    masterTree = new MasterTree();
    masterTree.load(tempPath)
  }
  then "the INDU.X node should be required", {
    masterTree.get("INDU.X").isRequired().shouldBe true
  }
  and "the BAC node should be required", {
    masterTree.get("BAC").isRequired().shouldBe true
  }
  and "the WFC node should not be required", {
    masterTree.get("WFC").isRequired().shouldBe false
  }
}

scenario "Source EODData should show up for GOOG", {
  given "I load a master tree with required nodes", {
    printExceptions {
      fixtureFileName = "master_tree_with_source.csv"
      masterTree = loadMasterTreeFixture(fixtureFileName)
    }
  }
  and "a temporary output path", {
    tempPath = tempFile().canonicalPath
    clearTmpDir()
  }
  then "GOOG should have be EODData source", {
    masterTree.get("GOOG").getSource().shouldBe "EODData"
  }
  and "after rwite to a temp and re-read, it should still read the source", {
    temp = tempFile().canonicalPath
    masterTree.write(temp)
    masterTree.load(temp)
    masterTree.get("GOOG").getSource().shouldBe "EODData"
  }
}

