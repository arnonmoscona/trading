package com.moscona.trading.persistence;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.exceptions.InvalidStateException;
import com.moscona.util.CsvHelper;
import com.moscona.util.StringHelper;
import com.moscona.util.TimeHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Arnon on 5/6/2014.
 * A database of historical splits data stored in a CSV file.
 * Specification is in the data collector requirements document draft 5 8/11/2010 or later
 */
public class SplitsDb {
    // FIXME SplitsDB should be defined by an interface for ease of mocking
    public static double MIN_RATIO_DIFFERENCE = 0.000001d;

    private String dbPath = null;
    private int size;
    private ConcurrentHashMap<String,ArrayList<Entry>> db = null;
    private String saveTo = null;

    public SplitsDb() {
        init();
    }

    private void init() {
        dbPath = null;
        size = 0;
        db = new ConcurrentHashMap<String,ArrayList<Entry>>();
    }

    public SplitsDb(String dbPath) throws IOException, InvalidArgumentException {
        this();
        load(dbPath);
    }

    public final void load(String dbPath) throws IOException, InvalidArgumentException {
        init();
        saveTo = dbPath;

        this.dbPath = dbPath;
        CsvHelper csvHelper = new CsvHelper();
        List<String[]> csv = csvHelper.load(dbPath);
        int i =0;
        for (String[] line: csv) {
            i++;
            if(i==1) {
                csvHelper.validateHeaders(line,new String[] {"Date","Symbol","Ratio","Problems","Usable flag","Last update"}, "splits database");
            }
            else {
                if (line == null || line.length==1 && StringUtils.isBlank(line[0])) {
                    continue; // ignore blank lines
                }
                add(new Entry(line));
            }
        }
    }

    public String getDefaultSaveTo() {
        return saveTo;
    }

    private void add(Entry entry) {
        String symbol = entry.getSymbol();
        if (! db.containsKey(symbol)) {
            db.put(symbol, new ArrayList<Entry>());
        }

        ArrayList<Entry> list = db.get(symbol);
        for (Entry member: list) {
            if (member.equals(entry)) {
                for (Problem problem: entry.getProblems()) {
                    member.getProblems().add(problem);
                    if (!(member.isUsable() && entry.isUsable())) {
                        member.setUsable(false);
                    }
                }
                member.setLastUpdate(TimeHelper.nowAsCalendar());
                return;
            }

            // they are not the same are they the same date?
            if (entry.getSymbol().equals(member.getSymbol()) && TimeHelper.isSameDay(entry.getDate(),member.getDate())) {
                member.setUsable(false);
                member.problems.add(new Problem(TimeHelper.nowAsCalendar(),
                        "observed two different ratios for the same split: "+member.getRatio()+
                                " and the new "+entry.getRatio()));
                member.setLastUpdate(TimeHelper.nowAsCalendar());
                return;
            }
        }

            list.add(entry);
        if (list.size() > 1) {
            Collections.sort(list, new Comparator<Entry>() {
                @Override
                public int compare(Entry o1, Entry o2) {
                    return (o1.getDate().compareTo(o2.getDate()));
                }
            });
        }
        size++;
        removeDuplicates(list);
    }

    /**
     * Removes duplicates, assuming that the list is already sorted
     * @param list
     */
    private void removeDuplicates(ArrayList<Entry> list) {
        Entry prev = null;
        Iterator<Entry> iterator = list.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.equals(prev)) {
                iterator.remove();
                size--;
            }
        }
    }

    public void add(String dateStr, String symbol, double ratio, String problems, boolean isUsable) throws InvalidArgumentException {
        add(new Entry(dateStr,symbol,ratio,problems,isUsable));
    }

    public void add(Calendar splitDate, String symbol, Float splitRatio, String problems, boolean usable) throws InvalidArgumentException {
        add(new Entry(splitDate,symbol,splitRatio,problems,usable));
    }

    public int size() {
        return size;
    }

    public List<Entry> get(String symbol) {
        return db.get(symbol);
    }

    public Set<String> getSymbols() {
        return db.keySet();
    }

    public void saveTo(String path) throws InvalidStateException, InvalidArgumentException, IOException {
        if (path==null) {
            throw new InvalidArgumentException("path may not be null");
        }
        if (! new File(path).getCanonicalFile().getParentFile().exists()) {
            throw new InvalidArgumentException("parent directory of the path given does not exist: "+path);
        }


        ArrayList<String> symbols = new ArrayList<String>(db.keySet());
        Collections.sort(symbols);
        try {
            PrintStream out = new PrintStream(new FileOutputStream(path));
            try {
                out.println("Date,Symbol,Ratio,Problems,Usable flag,Last update");
                for (String symbol: symbols) {
                    for (Entry split: db.get(symbol)) {
                        out.println(split.toString());
                    }
                }
            }
            finally {
                out.close();
            }
        }
        catch (Exception e) {
            throw new InvalidStateException("Exception while saving splits DB to "+path+": "+e,e);
        }
    }

    public void save() throws IOException, InvalidStateException, InvalidArgumentException {
        saveTo(saveTo);
    }

    public void setSaveTo(String saveTo) {
        this.saveTo = saveTo;
    }

    public Entry get(String symbol, Calendar day) {
        List<Entry> list = get(symbol);
        if (list==null || list.size()==0) {
            return null;
        }
        for (Entry entry: list) {
            if (TimeHelper.isSameDay(day, entry.date)) {
                return entry;
            }
        }
        return null;
    }

    public Entry getFirstSplitBetween(String symbol, Calendar lastDay, Calendar day) {
        List<Entry> list = get(symbol);
        if (list==null || list.size()==0) {
            return null;
        }
        for (Entry entry: list) {
            if (TimeHelper.isDayWithinRange(entry.getDate(), lastDay, day)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Given a known count on a stated date and a target date - calculates the correct stock count for the target date
     * @param count
     * @param symbol
     * @param target
     * @param countDate
     * @return
     */
    public long correctStockCount(long count, String symbol, Calendar target, Calendar countDate) {
        if (TimeHelper.isSameDay(target, countDate)) {
            return count;
        }
        int comp = target.compareTo(countDate);
        List<Entry> entries = get(symbol);
        if (entries==null || entries.size()==0) {
            return count;
        }

        double result = count;
        if (comp>=0) {
            // the target is after the date
            for (Entry entry: entries) {
                if (TimeHelper.isDayWithinRange(entry.getDate(),countDate, target)) {
                    result /= entry.getRatio();
                }
            }
        }
        else {
            // the target is before the date
            for (Entry entry: entries) {
                if (TimeHelper.isDayWithinRange(entry.getDate(),target, countDate)) {
                    result *= entry.getRatio();
                }
            }
        }

        return Math.round(result);
    }

    public boolean isLoaded() {
        return db != null;
    }

    //-----------------------------------------------------------------------------------------------------------------

    public class Entry {
        private Calendar date;
        private String symbol;
        private double ratio;
        private boolean isUsable;
        private List<Problem> problems;
        private Calendar lastUpdate;

        private Entry(String[] csvFields) throws InvalidArgumentException {
            if(csvFields.length != 6) {
                throw new InvalidArgumentException("splits DB fields must be exactly 6. Got "+csvFields.length+" in: \""+ StringHelper.join(csvFields, ",")+"\"");
            }
            int i=0;
            date = TimeHelper.parseMmDdYyyyDate(csvFields[i++]);
            symbol = csvFields[i++].trim();
            try {
                ratio = Double.parseDouble(csvFields[i++].trim());
            }
            catch (NumberFormatException e) {
                throw new InvalidArgumentException("Failed to parse split ratio \""+csvFields[2]+"\"");
            }
            String problemsStr = csvFields[i++];
            setProblems(problemsStr);
            isUsable = StringHelper.parseBoolean(csvFields[i++]);
            lastUpdate = TimeHelper.parseMmDdYyyyDate(csvFields[i++]);
        }

        private void setProblems(String problemsStr) throws InvalidArgumentException {
            problems = new ArrayList<Problem>();
            if (StringUtils.isNotBlank(problemsStr)) {
                String[] strArr = problemsStr.split(";");
                if (strArr.length % 2 != 0) {
                    throw new InvalidArgumentException("Failed to parse problems field. Uneven number of values in \""+problemsStr+"\"");
                }
                for (int j=0; j*2 < strArr.length; j++) {
                    problems.add(new Problem(TimeHelper.parseMmDdYyyyDate(strArr[j * 2].trim()), strArr[j*2+1].trim()));
                }
            }
        }

        public Entry(String dateStr, String symbol, double ratio) throws InvalidArgumentException {
            this(dateStr,symbol,ratio,"",true);
        }

        public Entry(String dateStr, String symbol, double ratio, String problems, boolean usable) throws InvalidArgumentException {
            this(dateStr,symbol,ratio,problems,usable,TimeHelper.nowAsCalendar());
        }

        public Entry(Calendar splitDate, String symbol, Float splitRatio, String problems, boolean usable) throws InvalidArgumentException {
            this(TimeHelper.toDayStringMmDdYyyy(splitDate), symbol, splitRatio, problems, usable);
        }

        public Entry(String dateStr, String symbol, double ratio, String problems, boolean usable, Calendar lastUpdate) throws InvalidArgumentException {
            date = TimeHelper.parseMmDdYyyyDate(dateStr);
            this.symbol = symbol.trim();
            this.ratio = ratio;
            this.isUsable = usable;
            setProblems(problems);
            this.lastUpdate = (Calendar)lastUpdate.clone();
        }

        public Calendar getDate() {
            return date;
        }

        public boolean isUsable() {
            return isUsable;
        }

        public void setUsable(boolean usable) {
            isUsable = usable;
        }

        public List<Problem> getProblems() {
            return problems;
        }

        public double getRatio() {
            return ratio;
        }

        public String getSymbol() {
            return symbol;
        }

        public Calendar getLastUpdate() {
            return (Calendar)lastUpdate.clone();
        }

        public void setLastUpdate(Calendar lastUpdate) {
            this.lastUpdate = (Calendar)lastUpdate.clone();
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            String comma = ",";
            s.append(TimeHelper.toDayStringMmDdYyyy(date)).append(comma);
            s.append(symbol).append(comma);
            s.append(Double.toString(ratio)).append(comma);
            s.append(StringUtils.join(problems,";")).append(comma);
            s.append(isUsable?"true":"false").append(comma);
            s.append(TimeHelper.toDayStringMmDdYyyy(lastUpdate));
            return s.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (! obj.getClass().equals(getClass())) {
                return false;
            }
            Entry other = (Entry)obj;
            return symbol.equals(other.symbol) && TimeHelper.isSameDay(date,other.date) && (Math.abs(ratio-other.ratio) < MIN_RATIO_DIFFERENCE);
        }
    }

    public class Problem {
        private Calendar date;
        private String problem;

        public Problem(Calendar date, String problem) {
            this.date = date;
            this.problem = problem;
        }

        public Calendar getDate() {
            return date;
        }

        public String getProblem() {
            return problem;
        }

        @Override
        public String toString() {
            return TimeHelper.toDayStringMmDdYyyy(date)+";"+problem.replaceAll(";","|").replaceAll(",","|");
        }
    }
}
