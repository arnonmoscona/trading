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
