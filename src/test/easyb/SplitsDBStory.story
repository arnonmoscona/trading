description "behavior of the splits history database"

import static com.moscona.test.easyb.TestHelper.*
import com.moscona.trading.persistence.SplitsDb
import com.moscona.test.easyb.MockTime

before_each "scenario", {
  given "an existing splits DB file path", {
    splitsPath = "${fixtures()}/splits.csv"
  }
  and "an empty splits Db instance", {
    printExceptions {
      splits = new SplitsDb()
    }
  }
  and "a TimeHelper set at 8/12/2010 17:00:00", {
    mockTime = new MockTime(date:"08/12/2010", hour:17, resetTimeHelper:true)
  }
}

after_each "scenario", {
  deleteAllRecursively(tmpDir())
}

scenario "default constructor", {
  then "no exceptions should happen with a default constructor", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
      }
    }
  }
}

scenario "constructor with load path", {
  then "no exceptions should happen with load path constructor", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb(splitsPath)
      }
    }
  }
}

scenario "default constructor followed by load", {
  then "no exceptions should happen with a default constructor followed by a load()", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  and "the Db should have 6 entries", {
    splits.size().shouldBe 6
  }
  and "AAPL should have 4 records", {
    splits.get("AAPL").size().shouldBe 4
  }
  and "XOM should have 1 record", {
    splits.get("XOM").size().shouldBe 1
  }
  and "the first entry for AAPL should be for 1999", {
    splits.get("AAPL")[0].toString().shouldBe "07/13/1999,AAPL,0.5,01/02/2010;conflicting data,false,08/11/2010"
  }
  and "the last entry for AAPL should be for 2007", {
    splits.get("AAPL")[3].toString().shouldBe "07/13/2007,AAPL,0.5,,true,08/11/2010"
  }
}

scenario "adding a record that already exists", {
  given "an existing db", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  when "I add a record that already matches an existing one", {
    splits.add("07/13/2007","AAPL",0.5,null,true)
  }
  then "the number of records does not change", {
    splits.size().shouldBe 6
    splits.get("AAPL").size().shouldBe 4
  }
  and "the record still looks the same", {
    splits.get("AAPL")[3].toString().shouldBe "07/13/2007,AAPL,0.5,,true,08/12/2010"
  }
}

scenario "adding a record that already exists but conflicts the existing one", {
  given "an existing db", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  when "I add a record that already matches an existing one except that the ratio is different", {
    splits.add("07/13/2007","AAPL",0.51,null,true)
  }
  then "the number of records does not change", {
    splits.size().shouldBe 6
    splits.get("AAPL").size().shouldBe 4
  }
  and "the record becomes unusable", {
    def aapl = splits.get("AAPL")[3]
    aapl.usable.shouldBe false
    aapl.problems[0].problem.shouldBe "observed two different ratios for the same split: 0.5 and the new 0.51"
  }
}

scenario "adding a record that was not there before", {
  given "an existing db", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  when "I add a record that already matches an existing one", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits.add("07/13/2010","AAPL",0.5,null,true)
        splits.add("07/13/2010","ATT",0.5,null,true)
      }
    }
  }
  then "the number of records does change", {
    splits.size().shouldBe 8
    splits.get("AAPL").size().shouldBe 5
    splits.get("ATT").size().shouldBe 1
  }
  and "the new record is added", {
    splits.get("AAPL")[4].toString().shouldBe "07/13/2010,AAPL,0.5,,true,08/12/2010"
    splits.get("ATT")[0].toString().shouldBe "07/13/2010,ATT,0.5,,true,08/12/2010"
  }
}

scenario "when saving the Db should be sorted by symbol and date", {
  given "an existing db", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  and "a temporary save location", {
    printExceptions {
      saveTo = tempFile()
      saveTo.delete()
      saveTo = saveTo.canonicalPath
    }
  }
  when "the file is saved to the location", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits.saveTo(saveTo)
      }
    }
  }
  then "one can load a new splits db from it", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        new SplitsDb(saveTo)
      }
    }
  }
  and "the file text should show it is sorted by symbol and date", {
    lines = new File(saveTo).readLines();
    expected = """Date,Symbol,Ratio,Problems,Usable flag,Last update
07/13/1999,AAPL,0.5,01/02/2010;conflicting data,false,08/11/2010
02/13/2005,AAPL,0.5,01/02/2009;failed to retrieve;02/03/2010;failed to retrieve,true,08/11/2010
01/02/2006,AAPL,0.6667,,true,08/11/2010
07/13/2007,AAPL,0.5,,true,08/11/2010
07/13/2010,BAC,0.5,,true,08/11/2010
06/21/2010,XOM,0.33333,,true,08/11/2010""".split("\n")
    lines.eachWithIndex {l,i->l.trim().shouldBe expected[i].trim()}
  }
}

scenario "save location from load", {
  given "an existing db", {
    ensureDoesNotThrow(Exception) {
      printExceptions {
        splits = new SplitsDb()
        splits.load(splitsPath)
      }
    }
  }
  then "the default save to location should be the location where the Db was loaded from", {
    splits.defaultSaveTo.shouldBe(splitsPath)
  }
}

