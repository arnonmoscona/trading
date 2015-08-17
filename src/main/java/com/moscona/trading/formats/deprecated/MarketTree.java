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

package com.moscona.trading.formats.deprecated;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.exceptions.InvalidStateException;
import com.moscona.util.CsvHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created: Mar 22, 2010 4:38:03 PM
 * By: Arnon Moscona
 * @deprecated this is inherited from an old project and really has too much ties to requirements of that project. Not generic enough and should be removed.
 */
@SuppressWarnings({"ReturnOfCollectionOrArrayField"})
@Deprecated
public class MarketTree implements Serializable {
    public static final String SPEC_VERSION = "0.1";
    public static final int MARKET_CLOSING_HOUR_FOR_STALENESS = 16;

    public static final String[] CSV_HEADERS = {
            "Type", "Name", "CoFullName", "Parent", "Shares", "Price", "PrevClose",
            "Weight", "AvgDayVol", "Class1", "CapRank","YearHigh", "YearLow"
    };
    private static final float WEIGHT_TOTAL = 1000f;


    private static final long serialVersionUID = -4291634139873917872L;
    private static final float WEIGHT_TOTAL_TOLERANCE = 0.1f;
    private HashMap<String, TreeEntry> tree = null;
    private HashMap<String,Integer> symbolToCodeMap = null;
    private HashMap<Integer,String> codeToSymbolMap = null;
    private String loadedFrom;
    private boolean isNeverStale; // for testability only
    private boolean isStaleWasCalled; // for testability only
    public static final String ROOT_NODE = "SP500";
    private MasterTree master = null;

    public MarketTree() {
        tree = new HashMap<String,TreeEntry>();
        codeToSymbolMap = new HashMap<Integer, String>();
        symbolToCodeMap = new HashMap<String, Integer>();
        loadedFrom = null;
        isNeverStale = false;
        isStaleWasCalled = false;
    }

    public AbstractMap<String, Integer> getSymbolToCodeMap() {
        return symbolToCodeMap;
    }

    public AbstractMap<Integer, String> getCodeToSymbolMap() {
        return codeToSymbolMap;
    }

    public int size() {
        return (tree==null)? 0 : tree.size();
    }

    public TreeEntry get(String symbol) {
        return tree.get(symbol);
    }

    public TreeEntry get(int code) throws InvalidArgumentException {
        String symbol = codeToSymbolMap.get(code);
        if (symbol == null) {
            throw new InvalidArgumentException("Code "+code+" does not exist in the market tree");
        }
        return get(symbol);
    }

    public MasterTree getMaster() {
        return master;
    }

    public void setMaster(MasterTree master) {
        this.master = master;
    }

    public boolean conformsToMaster() throws InvalidArgumentException {
        if (master==null) {
            throw new InvalidArgumentException("No master tree associated with this market tree");
        }

        int industryCount = 0;
        for (String name: tree.keySet()) {
            TreeEntry entry = get(name);


            MasterTree.TreeEntry masterEntry = master.get(name);
            if (masterEntry==null) {
                return false; // no corresponding master tree entry
            }

            if (entry.getType()== TreeEntry.INDUSTRY) {
                industryCount++;
            }
            else {
                if (! masterEntry.isOk()) {
                    return false; // all entries must be in OK status in the master tree for inclusion in market tree
                }
            }

            if (masterEntry.getType() != entry.getType()) {
                return false; // same name, different types
            }


            if (! masterEntry.getParent().equals(entry.getParent())) {
                return false; // different parent/child structure
            }
        }

        return master.countIndustries() == industryCount; // industries must be identical. Quotables, may be a subset
    }

    public void forceConformanceToMasterTree() throws InvalidStateException, InvalidArgumentException {
        if (conformsToMaster()) {
            return;
        }

        // does not conform
        // rebuild industry structure
        // remove any extra quotables

        ArrayList<String> toRemove = new ArrayList<String>();
        for (String name: tree.keySet()) {
            if (get(name).getType() == TreeEntry.INDUSTRY) {
                toRemove.add(name);
            }
            else {
                MasterTree.TreeEntry masterEntry = master.get(name);
                if (masterEntry==null || ! masterEntry.isOk()) {
                    toRemove.add(name);
                }
            }
        }

        for (String name: toRemove) {
            tree.remove(name);
        }

        for (MasterTree.TreeEntry industry: master.getIndustries()) {
            TreeEntry entry = new TreeEntry();
            entry.setType(TreeEntry.INDUSTRY);
            entry.setTree(this);
            entry.setName(industry.getName());
            entry.setFullName(industry.getFullName());
            entry.setParent(industry.getParent());
            tree.put(entry.getName(), entry);
        }


        // rebuild maps (e.g. symbol to code map)
        rebuildMaps();

        // rebuild weights
        rebuildWeights(master);
    }

    private void rebuildMaps() {
        symbolToCodeMap = new HashMap<String, Integer>();
        codeToSymbolMap = new HashMap<Integer, String>();
        int code = 0;
        for (TreeEntry entry: tree.values()) {
            entry.setCode(code);
            symbolToCodeMap.put(entry.getName(), code);
            codeToSymbolMap.put(code, entry.getName());
            code++;
        }
    }

    /**
     * Loads the market tree from a market tree file.
     * @param path the location of the file
     * @throws java.io.IOException on IO errors
     * @throws InvalidArgumentException if the file has problems
     * @throws com.moscona.exceptions.InvalidStateException if after loading we ended up with an invalid market tree
     * @return this (for easy chaining)
     */
    public MarketTree load(String path) throws IOException, InvalidArgumentException, InvalidStateException {
        loadedFrom = path;
        List<String[]> entries = new CsvHelper().load(path);

        if (entries.size()<10) {
            throw new InvalidArgumentException("The market tree file at "+path+" looks wrong as it has less than 10 rows...");
        }

        Iterator<String[]> it = entries.iterator();
        validateFirstRow(it.next());

        clear();

        int code = 0;
        while (it.hasNext()) {
            String[] row = it.next();
            TreeEntry entry = digestRow(row, code);
            add(code, entry);
            code++;
        }

        validate();

        return this;
    }

    private void add(int code, TreeEntry entry) throws InvalidArgumentException {
        if (entry.getType()>TreeEntry.MAX_TYPE || entry.getType()<TreeEntry.MIN_TYPE) {
            throw new InvalidArgumentException("Invalid type "+entry.getType()+" for attempted insertion into market tree of "+entry.getName());
        }
        String name = entry.getName();
        tree.put(name, entry);
        codeToSymbolMap.put(code, name);
        symbolToCodeMap.put(name,code);
        entry.setTree(this);
    }

    public synchronized void remove(String symbol) {
        TreeEntry entry = get(symbol);
        if (entry == null) {
            return;
        }
        int code = symbolToCodeMap.get(symbol);
        tree.remove(symbol);
        symbolToCodeMap.remove(symbol);
        codeToSymbolMap.remove(code);
    }

    public void validate() throws InvalidStateException {
        String problems = listStateProblems();
        if (StringUtils.isNotBlank(problems))
            throw new InvalidStateException(problems);
    }

    public boolean isValid() {
        return StringUtils.isBlank(listStateProblems());
    }

    public String listStateProblems() {
        String problems = "";

        if (codeToSymbolMap == null) {
            problems += " The codeToSymbolMap is null.";
        }
        if (symbolToCodeMap == null) {
            problems += " The symbolToCodeMap is null.";
        }
        if (tree == null) {
            problems += " The tree is null.";
        }

        if (StringUtils.isBlank(problems)) {
            if (codeToSymbolMap.size() != symbolToCodeMap.size()) {
                problems += " The codeToSymbolMap and symbolToCodeMap are not the same size... perhaps there are duplicate symbols?";
            }

            if (tree.size() != codeToSymbolMap.size()) {
                problems += " The tree and the maps are not the same size";
            }

            float sum = 0;

            // validate that the ranges make sense for each entry
            for (TreeEntry e : tree.values()) {
                if (e.type == TreeEntry.STOCK) {
                    sum += e.getWeight();
                }
                if (e.type == TreeEntry.STOCK || e.type == TreeEntry.INDEX) {
                    problems += listProblemsForStock(e);
                }
            }

            // validate that the weights add up to 1000
            float diff = sum - WEIGHT_TOTAL;
            if (diff < -WEIGHT_TOTAL_TOLERANCE || diff > WEIGHT_TOTAL_TOLERANCE) {
                problems += " The sum of the weights of stocks do not add up to the required " + WEIGHT_TOTAL + ", instead it is " + sum + " ...";
            }

            // validate relational integrity
            for (TreeEntry e : tree.values()) {
                String parent = e.getParent();
                if (! parent.equals(ROOT_NODE) && e.getType() != TreeEntry.INDEX) {
                    if (tree.get(parent) == null) {
                        problems +=  " The symbol "+ e.getName() + " has parent \"" + parent + "\", which is not found in the tree.";
                    }
                }
            }

            // validate required rows
            if (master != null) {
                for (MasterTree.TreeEntry masterEntry: master) {
                    String symbol = masterEntry.getName();
                    if (masterEntry.isRequired() && get(symbol)==null) {
                        problems += " The required symbol "+symbol+" is missing.";
                    }
                }
            }
        }

        return StringUtils.trim(problems);
    }

    public String listProblemsForStock(TreeEntry e) {
        String problems = "";
        if (e.getYearHigh() < e.getYearLow()) {
            problems += " For symbol " + e.getName() + " the 52 week high is lower than the 52 week low. ( 52 wk h "+e.getYearHigh()+" vs  52 wk l "+e.getYearLow()+")\n";
        }
        if (e.getPrice() > e.getYearHigh()) {
            problems += " For symbol " + e.getName() + " the 52 week high is lower than the last price. ( 52 wk h "+e.getYearHigh()+" vs pr "+e.getPrice()+")\n";
        }
        if (e.getPrice() < e.getYearLow()) {
            problems += " For symbol " + e.getName() + " the last price is lower than the 52 week low. ( pr "+e.getPrice()+" vs 52 wk l "+e.getYearLow()+")\n";
        }
        return problems;
    }

    /**
     * Clears al internal structures resulting in an empty market tree. It does not replace the instances.
     */
    public void clear() {
        tree.clear();
        codeToSymbolMap.clear();
        symbolToCodeMap.clear();
    }

    private TreeEntry digestRow(String[] row, Integer code) throws InvalidArgumentException {
        TreeEntry entry = new TreeEntry();
        entry.setCode(code);
        // "Type", "Name", "CoFullName", "Parent",
        //  "Shares", "Price", "PrevClose",
        // "Weight", "AvgDayVol", "Class1", "CapRank","YearHigh", "YearLow"
        int i = 0;
        entry.setType(row[i++]);
        entry.setName(row[i++]);
        entry.setFullName(row[i++]);
        entry.setParent(row[i++]);
        if (entry.getType() == TreeEntry.STOCK) {
            entry.setShares(row[i++]);
            entry.setPrice(row[i++]);
            entry.setPrevClose(row[i++]);
            entry.setWeight(row[i++]);
            entry.setAverageDailyVolume(row[i++]);
            entry.setClass1(row[i++]);
            entry.setCapRank(row[i++]);
            entry.setYearHigh(row[i++]);
            entry.setYearLow(row[i]);
        }
        if (entry.getType() == TreeEntry.INDEX) {
            i++; // skip shares
            entry.setPrice(row[i++]);
            entry.setPrevClose(row[i++]);
            i++; // skip weight
            i++; // skip volume
            i++; // skip class
            i++; // skip cap rank
            entry.setYearHigh(row[i++]);
            entry.setYearLow(row[i]);
        }

        return entry;
    }

    private void validateFirstRow(String[] headers) throws InvalidArgumentException {
        try {
            new CsvHelper().validateHeaders(headers, CSV_HEADERS, "market tree file");
        }
        catch (InvalidArgumentException e) {
            throw new InvalidArgumentException("Validation failed for "+loadedFrom+": "+e,e);
        }
    }

    /**
     * gets all the symbols that are of type stock
     * @return the list as an array
     */
    public String[] getStockSymbols() {
        return getSymbolsOfType(TreeEntry.STOCK);
    }

    /**
     * gets all the symbols that are of type index
     * @return the list as an array
     */
    public String[] getIndexSymbols() {
        return getSymbolsOfType(TreeEntry.INDEX);
    }

    /**
     * gets all the symbols that are of type stock or index
     * @return the list as an array
     */
    public String[] getQuotableSymbols() {
        String[] stocks =  getStockSymbols();
        String[] indexes =  getIndexSymbols();
        String[] retval = new String[stocks.length+indexes.length];
        int i=0;
        for (String s: indexes) {
            retval[i++] = s;
        }
        for (String s: stocks) {
            retval[i++] = s;
        }
        return retval;
    }

    /**
     * gets all the symbols that are of type industry
     * @return the list as an array
     */
    public String[] getIndustrySymbols() {
        return getSymbolsOfType(TreeEntry.INDUSTRY);
    }

    private String[] getSymbolsOfType(int symbolType) {
        Set<String> retval = new HashSet<String>();
        for (String symbol: tree.keySet()) {
            if (tree.get(symbol).getType() == symbolType) {
                retval.add(symbol);
            }
        }
        return retval.toArray(new String[0]);
    }

    public void add(MasterTree.TreeEntry masterEntry) throws InvalidArgumentException, CloneNotSupportedException {
        TreeEntry entry = new TreeEntry();

        entry.setType(masterEntry.getType());
        entry.setName(masterEntry.getName());
        entry.setFullName(masterEntry.getFullName());
        entry.setParent(masterEntry.getParent());

        add(entry);
    }

    public void add(TreeEntry entry) throws InvalidArgumentException, CloneNotSupportedException {
        if (entry.tree == this) {
            add(nextCode(), entry);
        }
        else {
            MarketTree.TreeEntry newEntry = entry.clone();
            newEntry.tree = this;
            add(nextCode(), newEntry);
        }
    }

    /**
     * Finds the next unused code
     * @return a number greater than any code used
     */
    private int nextCode() {
        int retval = 0;
        for (Integer code: codeToSymbolMap.keySet()) {
            retval = Math.max(retval,code);
        }
        return retval+1;
    }

    public boolean isStale() throws InvalidStateException {
        if (loadedFrom==null) {
            throw new InvalidStateException("Market tree not loaded!");
        }

        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(new File(loadedFrom).lastModified());
        Calendar now = Calendar.getInstance();
        return isStale(lastModified, now);
    }

    /**
     * Checks for staleness given a timestamp in the past and a timestamp for now.
     * Separate method for testability.
     * @param lastModified
     * @param now
     * @return
     */
    public boolean isStale(Calendar lastModified, Calendar now) {
        isStaleWasCalled = true;

        if (isNeverStale) {
            return false;
        }

        if (now.getTimeInMillis()-lastModified.getTimeInMillis() > 24*3600000) {
            return true; // more than 24 hour difference
        }

        int marketCloseHour = MARKET_CLOSING_HOUR_FOR_STALENESS;

        // within 24 hours but is is before yesterday's close?
        if (DateUtils.isSameDay(lastModified, now)) {
            // same day so stale only if one was before the closing and the other after
            return (now.get(Calendar.HOUR_OF_DAY) >= marketCloseHour && lastModified.get(Calendar.HOUR_OF_DAY)<marketCloseHour);
        }

        // different days so only care if the last was before closing or not
        return lastModified.get(Calendar.HOUR_OF_DAY)<marketCloseHour;

        // FIXME ignores weekends and holidays...
    }

    public TreeEntry newTreeEntry() {
        return new TreeEntry();
    }

    /**
     * @return the number of quotable items in the market tree
     */
    public int countQuotables() {
        int retval = 0;
        for (TreeEntry entry : tree.values()) {
            if (entry.isQuotable()) {
                retval++;
            }
        }
        return retval;
    }

    public void applySplit(String symbol, Double splitRatio) throws InvalidArgumentException {
        TreeEntry entry = tree.get(symbol);
        if (entry==null) {
            return;
        }
        if (entry.getType()==TreeEntry.STOCK) {
            entry.setShares((long)((double)entry.getShares()/splitRatio));
        }
        entry.setPrice((float)(entry.getPrice()*splitRatio));
        entry.setYearHigh((float)(entry.getYearHigh()*splitRatio));
        entry.setYearLow((float)(entry.getYearLow()*splitRatio));
    }

    /**
     * Removes the symbol from the market tree, leaving the tree in an invalid state (needs recalculation of weights etc)
     * @param symbol
     */
    public void delete(String symbol) {
        tree.remove(symbol);
        if (symbolToCodeMap.containsKey(symbol)) {
            int code = symbolToCodeMap.get(symbol);
            symbolToCodeMap.remove(symbol);
            codeToSymbolMap.remove(code);
        }
        rebuildMaps();
    }



    public void rebuildWeights(MasterTree master) throws InvalidArgumentException, InvalidStateException {
        ArrayList<MarketTree.TreeEntry> list = new ArrayList<MarketTree.TreeEntry>();
        float totalMarketCap = 0.0f;

        // loop 1: calculate total market cap
        for(String symbol : getSymbolToCodeMap().keySet()) {
            MarketTree.TreeEntry marketTreeEntry = get(symbol);
            list.add(marketTreeEntry);
            if (marketTreeEntry.getType() == MarketTree.TreeEntry.STOCK) {
                // we calculate weights only for stocks
                float marketCap = marketTreeEntry.getMarketCap();
                totalMarketCap += marketCap;
            }
        }

        // loop 2: calculate weights
        for(String symbol : getSymbolToCodeMap().keySet()) {
            MarketTree.TreeEntry marketTreeEntry = get(symbol);
            if (marketTreeEntry.getType() == MarketTree.TreeEntry.STOCK) {
                // we calculate weights only for stocks
                float marketCap = marketTreeEntry.getMarketCap();
                marketTreeEntry.setWeight(1000.0f * marketCap / totalMarketCap);
            }
        }

        // loop 3: recalculate cap ranks
        Collections.sort(list, new Comparator<MarketTree.TreeEntry>() {
            @Override
            public int compare(MarketTree.TreeEntry o1, MarketTree.TreeEntry o2) {
                return (int)Math.signum(o2.getMarketCap()-o1.getMarketCap());
            }
        });
        int rank = 0;
        for(MarketTree.TreeEntry e:list) {
            if (e.getType() == MarketTree.TreeEntry.STOCK){
                e.setCapRank(++rank);
            }
        }
    }

    /**
     * returns a flattened list of all stock descendants of an industry node
     * @param name
     * @return
     */
    public List<String> getStockDescendantsOf(String name) throws InvalidArgumentException {
        ArrayList<String> retval = new ArrayList<String>();
        if (get(name).getType() != TreeEntry.INDUSTRY) {
            throw new InvalidArgumentException("The node \""+name+"\" is not an industry node");
        }

        HashSet<TreeEntry> children = new HashSet<TreeEntry>();
        for (TreeEntry entry: tree.values()) {
            if (entry.getParent().equals(name)) {
                if (entry.getType() == TreeEntry.STOCK) {
                    retval.add(entry.getName());
                }
                else if(entry.getType() == TreeEntry.INDUSTRY) {
                    // recursive descent
                    retval.addAll(getStockDescendantsOf(entry.getName()));
                }
            }
        }
        return retval;
    }

    public String getRootNodeName() {
        return ROOT_NODE;
    }

    public List<String> getChildrenOf(String name) {
        ArrayList<String> retval = new ArrayList<String>();
        for (TreeEntry entry: tree.values()) {
            if (entry.getParent().equals(name)) {
                retval.add(entry.getName());
            }
        }
        return retval;
    }

    public static class TreeEntry implements Cloneable, Comparable<TreeEntry>, Serializable {
        public static final int INDUSTRY = 1;
        public static final int INDEX = 2;
        public static final int STOCK = 3;
        public static final int MIN_TYPE = INDUSTRY;
        public static final int MAX_TYPE = STOCK;
        private static final long serialVersionUID = 2642082275641071366L;

        private int code=-1;
        private int type; // STOCK or INDUSTRY
        private String name;
        private String fullName;
        private String parent; // name of parent
        private long shares;
        private float price; // closing price of previous (trading) day
        private float weight;
        private long averageDailyVolume;
        private String class1; // small, mid, large cap
        private int capRank;
        private float yearHigh; // 52 week high
        private float yearLow; // 52 week low
        private float marketCap;
        private MarketTree tree = null;
        private HashMap<String,Object> decorations;

        public TreeEntry() {
            decorations = new HashMap<String, Object>();
        }

        public void setTree(MarketTree tree) {
            this.tree = tree;
        }

        public TreeEntry copy(TreeEntry other) {
            code = other.code;
            type = other.type;
            name = other.name;
            fullName = other.fullName;
            parent = other.parent;
            shares = other.shares;
            price = other.price;
            weight = other.weight;
            averageDailyVolume = other.averageDailyVolume;
            class1 = other.class1;
            capRank = other.capRank;
            yearHigh = other.yearHigh;
            yearLow = other.yearLow;
            decorations = new HashMap<String, Object>(other.decorations);

            return this;
        }

        public HashMap<String, Object> getDecorations() {
            return decorations;
        }

        @Override
        protected TreeEntry clone() throws CloneNotSupportedException {
            TreeEntry clone = (TreeEntry) super.clone();
            clone.decorations = new HashMap<String, Object>(decorations);
            return clone;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(getTypeName(type)).append(",").append(name).append(",");
            s.append(getFullName()).append(",").append(parent).append((","));
            if (getType()!=INDUSTRY) {
                s.append(price).append(",").append(capRank);
            }
            return s.toString();
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) throws InvalidArgumentException {
            if (type != STOCK && type != INDUSTRY && type != INDEX) {
                throw new InvalidArgumentException("Invalid type: must be STOCK, INDEX or INDUSTRY");
            }
            this.type = type;
        }

        public void setType(String type) throws InvalidArgumentException {
            setType(stringToType(type));
        }

        public int stringToType(String type) throws InvalidArgumentException {
            String typeName = trim(type, "Type", true);
            if (typeName.equals("Industry")) {
                return(INDUSTRY);
            }
            else if (typeName.equals("Stock")) {
                return(STOCK);
            }
            else if (typeName.equals("Index")) {
                return(INDEX);
            }
            else throw new InvalidArgumentException("Invalid value for Type: '"+type+"'");
        }

        public String getName() {
            return name;
        }

        public void setName(String name) throws InvalidArgumentException {
            this.name = trim(name, "name", true);
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) throws InvalidArgumentException {
            this.fullName = trim(fullName,"CoFullName",type!=INDUSTRY);
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) throws InvalidArgumentException {
            this.parent = trim(parent,"Parent",type!=INDEX);
        }

        public long getShares() {
            return shares;
        }

        public void setShares(long shares) throws InvalidArgumentException {
            this.shares = requirePositive(shares, "Shares");
        }

        public void setShares(String shares) throws InvalidArgumentException {
            if (type==INDUSTRY || type==INDEX) return;
            setShares(Long.parseLong(shares));
        }

        public float getPrice() {
            return price;
        }

        public void setPrice(float price) throws InvalidArgumentException {
            this.price = requirePositive(price, "Price");
        }

        public void setPrice(String price) throws InvalidArgumentException {
            setPrice(Float.parseFloat(price));
        }

        public float getPrevClose() {
            return price;
        }

        public void setPrevClose(float prevClose) throws InvalidArgumentException {
            this.price = requirePositive(prevClose, "PrevClose");
        }

        public void setPrevClose(String prevClose) throws InvalidArgumentException {
            if (!StringUtils.isBlank(prevClose)) {
                setPrevClose(Float.parseFloat(prevClose));
            }
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) throws InvalidArgumentException {
            if (weight < 0.0f || weight > 1000.0f) {
                throw new InvalidArgumentException("Weight must be between 0.0 and 1000.0 - got "+weight);
            }
            this.weight = weight;
        }

        public void setWeight(String weight) throws InvalidArgumentException {
            setWeight(Float.parseFloat(weight));
        }

        public long getAverageDailyVolume() {
            return averageDailyVolume;
        }

        public void setAverageDailyVolume(long averageDailyVolume) throws InvalidArgumentException {
            if (type==INDEX) {
                return; // we do not support average volume for index
            }
            this.averageDailyVolume = averageDailyVolume;
        }

        public void setAverageDailyVolume(String averageDailyVolume) throws InvalidArgumentException {
            if (type==INDEX) {
                return; // we do not support average volume for index
            }
            setAverageDailyVolume(Long.parseLong(averageDailyVolume));
        }

        public String getClass1() {
            return class1;
        }

        public void setClass1(String class1) {
            if (type==INDEX) {
                return;
            }
            if (! StringUtils.isBlank(class1)) {
                this.class1 = StringUtils.trim(class1);
            }
        }

        public int getCapRank() {
            return capRank;
        }

        public void setCapRank(int capRank) throws InvalidArgumentException {
            this.capRank = (int) requirePositive(capRank,"CapRank");
        }

        public void setCapRank(String capRank) throws InvalidArgumentException {
            setCapRank(Integer.parseInt(capRank));
        }

        public float getYearHigh() {
            return yearHigh;
        }

        public void setYearHigh(float yearHigh) throws InvalidArgumentException {
            this.yearHigh = requirePositive(yearHigh,"YearHigh");
        }

        public void setYearHigh(String yearHigh) throws InvalidArgumentException {
            setYearHigh(Float.parseFloat(yearHigh));
        }

        public float getYearLow() {
            return yearLow;
        }

        public void setYearLow(float yearLow) throws InvalidArgumentException {
            this.yearLow = requirePositive(yearLow,"YearLow");
        }

        public void setYearLow(String yearLow) throws InvalidArgumentException {
            setYearLow(Float.parseFloat(yearLow));
        }

        // validation methods

        protected String trim(String str, String field, boolean required) throws InvalidArgumentException {
            if (StringUtils.isBlank(str) && required) {
                throw new InvalidArgumentException(field+" may not be blank for code "+code);
            }
            return StringUtils.trim(str);
        }

        private long requirePositive(long number, String field) throws InvalidArgumentException {
            if (number <= 0) {
                throw new InvalidArgumentException(field+" must be positive for "+name+" (got "+number+")");
            }
            return number;
        }

        private float requirePositive(float number, String field) throws InvalidArgumentException {
            if (number <= 0.0) {
                throw new InvalidArgumentException(field+" must be positive for "+name+" (got "+number+")");
            }
            return number;
        }

        public float getMarketCap() {
            if (marketCap < 0.1f) {
                marketCap = getShares()*getPrice();
            }
            return marketCap;
        }

        public void setMarketCap(float marketCap) {
            this.marketCap = marketCap;
        }

        public static String getTypeName(int type) {
            switch(type) {
                case STOCK:
                    return "Stock";
                case INDUSTRY:
                    return "Industry";
                case INDEX:
                    return "Index";
                default:
                    return "INVALID TYPE: "+type;
            }
        }


        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * <p/>
         * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
         * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
         * <tt>y.compareTo(x)</tt> throws an exception.)
         * <p/>
         * <p>The implementor must also ensure that the relation is transitive:
         * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
         * <tt>x.compareTo(z)&gt;0</tt>.
         * <p/>
         * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
         * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
         * all <tt>z</tt>.
         * <p/>
         * <p>It is strongly recommended, but <i>not</i> strictly required that
         * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
         * class that implements the <tt>Comparable</tt> interface and violates
         * this condition should clearly indicate this fact.  The recommended
         * language is "Note: this class has a natural ordering that is
         * inconsistent with equals."
         * <p/>
         * <p>In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         *         is less than, equal to, or greater than the specified object.
         * @throws ClassCastException if the specified object's type prevents it
         *                            from being compared to this object.
         */
        @Override
        @SuppressWarnings({"MethodWithMultipleReturnPoints"})
        public int compareTo(TreeEntry o) {
            if (type!=o.getType()) {
                return type-o.type; // order determined by the oder implied by the constant values
            }
            else {
                // same type
                switch(type) {
                    case INDUSTRY:
                        // industries are ordered by parent-child relationship
                        if (isChildOf(o)) {
                            return 1; // children after parents
                        }
                        else if (o.isChildOf(this)) {
                            return -1; // parents before children
                        }
                        else if (depth() != o.depth()) {
                            return depth()-o.depth();
                        }
                        else {
                            // otherwise - alphabetical order
                            return (parent+"|"+name).compareTo(o.parent+"|"+o.name);
                        }
                    case STOCK:
                        return capRank-o.getCapRank(); // stocks compared by cap rank
                    default:
                        return 0;
                }
            }
        }

        public int depth() {
            if (tree==null) {
                return 0;
            }
            if (name.equals(ROOT_NODE)) {
                return 0;
            }

            String parent = getParent();
            if (parent.equals(name)) {
                return 0;
            }
            int retval = 0;
            while(!parent.equals(name) && !parent.equals(ROOT_NODE)) {
                retval++;
                TreeEntry parentNode = tree.get(parent);
                if (parentNode==null) {
                    return retval;
                }
                parent = parentNode.getParent();
            }

            return retval;
        }

        /**
         * Determines whether the node is a child of the other node.
         * @param other
         * @return true if there is a market tree and the statement is true. False otherwise
         */
        public boolean isChildOf(TreeEntry other) {
            if(tree == null) {
                return false; // we don't know so we say no
            }
            if (other.getName().equals(ROOT_NODE)) {
                return true;
            }
            if (other.getName().equals(name)) {
                return false;
            }


            String parent = this.parent;
            while (!parent.equals(ROOT_NODE)) {
                if (parent.equals(other.getName())) {
                    return true;
                }
                TreeEntry parentNode = tree.get(parent);
                if (parentNode == null) {
                    return false;
                }
                parent = parentNode.getParent();
            }
            return false;
        }

        public String[] toStrings() throws InvalidStateException {
            /*
            "Type", "Name", "CoFullName", "Parent", "Shares", "Price", "PrevClose",
            "Weight", "AvgDayVol", "Class1", "CapRank","YearHigh", "YearLow"
             */
            switch(type) {
                case INDUSTRY:
                    return new String[] {
                        getTypeName(type),
                        name.trim(),
                        fullName.trim(),
                        parent.trim(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                    };
                case STOCK:
                    return new String[] {
                        getTypeName(type),
                        name.trim(),
                        fullName.trim(),
                        parent.trim(),
                        Long.toString(shares),
                        twoDigitFloatToString(price),
                        "",
                        Float.toString(weight),
                        Long.toString(averageDailyVolume),
                        class1,
                        Integer.toString(capRank),
                        twoDigitFloatToString(yearHigh),
                        twoDigitFloatToString(yearLow)
                    };
                case INDEX:
                    return new String[] {
                        getTypeName(type),
                        name.trim(),
                        fullName.trim(),
                        "",
                        "",
                        twoDigitFloatToString(price),
                        "",
                        "",
                        "",
                        "",
                        "",
                        twoDigitFloatToString(yearHigh),
                        twoDigitFloatToString(yearLow)
                    };

                default:
                throw new InvalidStateException("encountered a record with an unknown type: "+type+" ("+name+")");
            }
        }

        private String twoDigitFloatToString(float price) {
            float factor = 100;
            return Float.toString(Math.round(price*factor)/factor);
        }


        public boolean isQuotable() {
            return type == STOCK || type == INDEX;
        }

        public MarketTree getTree() {
            return tree;
        }

        public String getIndustry() {
            return getParent();
        }

        public String getSector() {
            String industry = getIndustry();
            if (industry==null || tree.get(industry)==null) {
                return null;
            }
            return tree.get(industry).getParent();
        }
    }

    public List<String> listSymbolsOfType(int symbolType) {
        ArrayList<String> retval = new ArrayList<String>();
        for (String symbol: tree.keySet()) {
            if (tree.get(symbol).getType() == symbolType) {
                retval.add(symbol);
            }
        }
        return retval;
    }

    public void write(String path) throws InvalidStateException, IOException {
        ArrayList<TreeEntry> list = new ArrayList<TreeEntry>();
        for (TreeEntry entry: tree.values()) {
            list.add(entry);
        }

        Collections.sort(list);

        ArrayList<String[]> output = new ArrayList<String[]>();
        for (TreeEntry entry: list) {
            output.add(entry.toStrings());
        }

        new CsvHelper().writeAll(path, CSV_HEADERS, output);
    }

    /**
     * For testability only, can be used to make it never stale
     * @param neverStale
     */
    public void setNeverStale(boolean neverStale) {
        isNeverStale = neverStale;
    }

    public boolean isNeverStale() {
        return isNeverStale;
    }

    public boolean isStaleWasCalled() {
        return isStaleWasCalled;
    }

    protected void setTree(AbstractMap<String, TreeEntry> tree) {
        this.tree = new HashMap<String, TreeEntry>();
        this.tree.putAll(tree);
    }

    public String getLoadedFrom() {
        return loadedFrom;
    }
}

