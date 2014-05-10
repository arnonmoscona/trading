package com.moscona.test.easyb

import com.moscona.trading.formats.deprecated.MarketTree
import com.moscona.trading.formats.deprecated.MasterTree
import static com.moscona.test.easyb.TestHelper.*

/**
 * Created by Arnon on 5/9/2014.
 */
class MarketTreeTestHelper {
  static loadMarketTreeFixture(master="market_tree.csv", working="market_tree.csv") {
    String fixture_file = copyFromMaster(master, working)
    def valid_tree = null;
    printExceptions {
      valid_tree = new MarketTree().load(fixture_file)
    }
    valid_tree
  }

  static loadMasterTreeFixture(master="master_tree.csv", working="master_tree.csv") {
    String fixture_file = copyFromMaster(master, working)
    def valid_tree = null;
    printExceptions {
      valid_tree = new MasterTree().load(fixture_file)
    }
    valid_tree
  }
}
