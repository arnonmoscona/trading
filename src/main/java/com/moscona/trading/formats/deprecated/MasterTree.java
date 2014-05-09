package com.moscona.trading.formats.deprecated;

import com.google.common.base.Strings;
import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.exceptions.InvalidStateException;
import com.moscona.trading.excptions.MissingSymbolException;
import com.moscona.util.CsvHelper;
import com.moscona.util.ExceptionHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created: Jun 8, 2010 4:46:28 PM
 * By: Arnon Moscona
 * Implements the master tree - the master list used to generate the market tree and track symbol status.
 * See requirements in Data Collector Requirements V0.3 or later
 *
 * @deprecated this must go away, along with MarketTree
 * @see com.moscona.trading.formats.deprecated.MarketTree
 */
@Deprecated
public class MasterTree implements Iterable<MasterTree.TreeEntry> {
    public static final String SPEC_VERSION = "0.5";

    public static void copySourceToMasterEntry(MarketTree.TreeEntry entry, TreeEntry masterEntry) {
        Object source = entry.getDecorations().get("source");
        if(source != null) {
            masterEntry.setSource("" + source);
            masterEntry.setLastUpdate(Calendar.getInstance());
        }
    }

    public enum FormatVersion {
        SPEC_0_3,   // conforming to spec version 0.3
        SPEC_0_5,   // conforming to spec version 0.5 (added support for required lines)
        SPEC_0_5_1  // conforming to spec version 0.5 (added support for required and Source columns)
    }

    public static final String[] CSV_HEADERS = {
            "Type", "Name", "CoFullName", "Parent", "Class1", "Status","Error", "LastUpdate"
    };

    // The following adds (optional) support for required quotables
    public static final String[] CSV_HEADERS_V2 = {
            "Type", "Name", "CoFullName", "Parent", "Class1", "Status","Error", "LastUpdate", "Required"
    };

    public static final String[] CSV_HEADERS_V3 = {
            "Type", "Name", "CoFullName", "Parent", "Class1", "Status","Error", "LastUpdate", "Required", "Source"
    };

    public static final String[] ROOT_NAMES = { "SP500" };
    public static final String[] STATUS_NAMES = { "OK", "stale", "error", "new", "deleted", "disabled" };

    public MasterTree() {
        clear();
    }

    private ArrayList<TreeEntry> rows;
    private ArrayList<TreeEntry> nonDeletedRows;
    private ArrayList<TreeEntry> visibleRows;
    private HashMap<String,TreeEntry> tree;
    private String loadedFrom;
    private int disabledCount = 0;
    private FormatVersion formatVersion = FormatVersion.SPEC_0_3;

    public MasterTree load(String path) throws IOException, InvalidArgumentException, InvalidStateException {
        List<String[]> entries = new CsvHelper().load(path);

        if (entries.size()<10) {
            throw new InvalidArgumentException("The market tree file at "+path+" looks wrong as it has less than 10 rows...");
        }

        Iterator<String[]> it = entries.iterator();
        String[] headers = it.next();
        try {
            formatVersion = FormatVersion.SPEC_0_3;
            new CsvHelper().validateHeaders(headers, CSV_HEADERS, "master tree file");
        }
        catch (InvalidArgumentException e) {
            try {
                formatVersion = FormatVersion.SPEC_0_5;
                new CsvHelper().validateHeaders(headers, CSV_HEADERS_V2, "master tree file");
            }
            catch (InvalidArgumentException e1) {
                formatVersion = FormatVersion.SPEC_0_5_1;
                new CsvHelper().validateHeaders(headers, CSV_HEADERS_V3, "master tree file");
            }
        }

        clear();

        while (it.hasNext()) {
            String[] row = it.next();
            TreeEntry entry = digestRow(row, formatVersion);
            String name = entry.getName();
            rows.add(entry);
            tree.put(name, entry);
            if (! entry.getStatus().equals("deleted")) {
                nonDeletedRows.add(entry);
                if (entry.getStatus().equals("disabled")) {
                    disabledCount++;
                }
                else {
                    visibleRows.add(entry);
                }
            }
        }

        validate();
        loadedFrom = path;

        return this;
    }

    private TreeEntry digestRow(String[] row, FormatVersion formatVersion) throws InvalidArgumentException {
        TreeEntry entry = new TreeEntry();
        int i=0;

        // "Type", "Name", "CoFullName", "Parent", "Class1", "Status","Error", "LastUpdate"
        entry.setType(row[i++]);
        entry.setName(row[i++]);
        entry.setFullName(row[i++]);
        entry.setParent(row[i++]);
        entry.setClass1(row[i++]);
        entry.setStatus(row[i++]);
        entry.setError(row[i++]);
        entry.setLastUpdate(row[i++]);
        if ((formatVersion == FormatVersion.SPEC_0_5 || formatVersion == FormatVersion.SPEC_0_5_1) && row.length >= CSV_HEADERS_V2.length) {
            entry.setIsRequired(row[i++].toLowerCase().equals("required"));
        }
        if (formatVersion == FormatVersion.SPEC_0_5_1 && row.length == CSV_HEADERS_V3.length) {
            entry.setSource(row[i++]);
        }
        return entry;
    }

    private void validate() throws InvalidStateException {
        String problems = listStateProblems();
        if (StringUtils.isNotBlank(problems))
            throw new InvalidStateException(problems);
    }

    private String listStateProblems() {
        String problems = "";

        if (rows == null) {
            problems += "the internal row variable is null. ";
        }
        if (rows!=null && rows.size() == 0) {
            problems += "the internal row variable is empty. ";
        }
        if (tree == null) {
            problems += "the internal tree variable is null. ";
        }
        if (tree!=null && rows != null && tree.size() != rows.size()) {
            problems += "the internal tree variable has a different number of entries than the internal rows variable. ";
        }
        if (rows == null || tree==null) {
            return problems; // cannot check any further
        }

        for (TreeEntry entry: rows) {
            if (StringUtils.isBlank(entry.getName())) {
                problems += "Found a row with an empty name! ";
            }

            String name = entry.getName();
            if (StringUtils.isBlank(entry.getFullName())) {
                problems += "The row for "+name+" has an empty full name. ";
            }
            if (entry.getType()!=MarketTree.TreeEntry.INDEX) { // no parent validation for index
                String parent = entry.getParent();
                if (StringUtils.isBlank(parent)) {
                    problems += "The row for "+name+" has no parent. ";
                }
                if (StringUtils.isNotBlank(parent) && !Arrays.asList(ROOT_NAMES).contains(parent) && ! tree.containsKey(parent)) {
                    problems += "The row for "+name+" has a parent. But "+parent+" is not an allowed root name and is not another entry name in the list. ";
                }
            }
            int type = entry.getType();
            String status = entry.getStatus();
            if (!Arrays.asList(STATUS_NAMES).contains(status) && isQuotable(type)) {
                problems += "The row for "+name+" has an invalid status: "+status+" ";
            }
            if (entry.getLastUpdate()==null && isQuotable(type) && !status.equals("new")) {
                problems += "The row for "+name+" has no last update value. ";
            }
        }

        return problems;
    }

    private boolean isQuotable(int type) {
        return (type == MarketTree.TreeEntry.STOCK);
    }

    public List<String> getQuotableSymbols() {
        ArrayList<String> list = new ArrayList<String>();
        for (TreeEntry entry: rows) {
            if (entry.isQuotable() && !entry.isDisabled() && ! entry.isDeleted()) {
                list.add(entry.getName());
            }
        }
        return list;
    }

    private void clear() {
        rows = new ArrayList<TreeEntry>();
        nonDeletedRows = new ArrayList<TreeEntry>();
        visibleRows = new ArrayList<TreeEntry>();
        tree = new HashMap<String,TreeEntry>();
    }

    public int size() {
        if (rows==null) {
            return 0;
        }
        return rows.size()-disabledCount;
    }

    /**
     * Returns an iterator over a set of elements of type T.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<TreeEntry> iterator() {
        if (visibleRows == null) {
            return null;
        }
        return visibleRows.iterator();
    }

    public TreeEntry get(String name) {
        return tree.get(name);
    }


    public int getDisabledCount() {
        return disabledCount;
    }

    public void write(String path) throws InvalidStateException, IOException {
        validate();

        ArrayList<String[]> output = new ArrayList<String[]>();
        for (TreeEntry entry: nonDeletedRows) {
            output.add(entry.toStrings());
        }
        new CsvHelper().writeAll(path, CSV_HEADERS_V3, output);
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints"})
    public boolean equals(MasterTree other) throws InvalidStateException {
        if (other==null) {
            return false;
        }

        validate();
        other.validate();

        if (size() != other.size()) {
            return false;
        }

        for (TreeEntry entry: other.nonDeletedRows) {
            TreeEntry myEntry = tree.get(entry.getName());
            if (myEntry==null) {
                return false;
            }
            if (!entry.equals(myEntry)) {
                return false;
            }
        }
        return true;
    }

    public int countQuotables() {
        int retval = 0;
        for (TreeEntry e: rows) {
            if (isQuotable(e.getType())) {
                retval+= 1;
            }
        }
        return retval;
    }

    public int countIndustries() {
        int retval = 0;
        for (TreeEntry e: rows) {
            if (e.getType() == MarketTree.TreeEntry.INDUSTRY) {
                retval+= 1;
            }
        }
        return retval;
    }

    public ArrayList<TreeEntry> getIndustries() {
        ArrayList<TreeEntry> industries = new ArrayList<TreeEntry>();
        for (TreeEntry e: rows) {
            if (e.getType() == MarketTree.TreeEntry.INDUSTRY) {
                industries.add(e);
            }
        }
        return industries;
    }

    public List<String> getErrorSummary() {
        ArrayList<String> summary = new ArrayList<String>();
        for (TreeEntry entry:rows) {
            if (entry.isQuotable() && entry.getStatus().equals("error")) {
                summary.add(entry.getName()+" ("+entry.getFullName()+"): "+entry.getError());
            }
        }
        return summary;
    }

    @SuppressWarnings({"InstanceVariableMayNotBeInitialized"})
    public static class TreeEntry {
        private int type; // same as in MarketTree.TreeEntry
        private String name;
        private String fullName;
        private String parent;
        private String class1; // small, mid, large cap
        private String status; // OK/error/stale/new/deleted as per spec
        private String error;
        private Calendar lastUpdate;
        private boolean isRequired = false;
        private String source; // IQFeed or EODData

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setType(String type) throws InvalidArgumentException {
            setType(new MarketTree().newTreeEntry().stringToType(type));  // changed in attempt to get rid of compilation failure in team city
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getClass1() {
            return class1;
        }

        public void setClass1(String class1) {
            if (! StringUtils.isBlank(class1)) {
                this.class1 = StringUtils.trim(class1);
            }
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Calendar getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(Calendar lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public void setLastUpdate(String lastUpdate) throws InvalidArgumentException {
            if (StringUtils.isBlank(lastUpdate)) {
                this.lastUpdate = null;
                return;
            }

            String[] formats = {
                    "MM/dd/yy HH:mm:ss a z",
                    "MM/dd/yy HH:mm a z",
                    "MM/dd/yy HH:mm:ss a",
                    "MM/dd/yy HH:mm a",
                    "MM/dd/yy HH:mm:ss",
                    "MM/dd/yy HH:mm",
                    "MM/dd/yy",
            };

            Calendar result = null;
            for (String format: formats) {
                try {
                    Date date = new SimpleDateFormat(format).parse(lastUpdate);
                    result = Calendar.getInstance();
                    result.setTime(date);
                    setLastUpdate(result);
                    return;
                }
                catch (ParseException e) {
                    // ignore and move on to the next format
                }
            }
            throw new InvalidArgumentException("Could not parse last update value: '"+lastUpdate+"'");
        }

        public boolean isQuotable() {
            return type==MarketTree.TreeEntry.STOCK || type==MarketTree.TreeEntry.INDEX;
        }

        public boolean isRequired() {
            return isRequired;
        }

        public void setIsRequired(boolean requiredFieldValue) {
            isRequired = requiredFieldValue;
        }


        public String[] toStrings() {
            ArrayList<String> out = new ArrayList<String>();
            out.add(MarketTree.TreeEntry.getTypeName(type));
            out.add(name.trim());
            out.add(fullName.trim());
            out.add(parent.trim());

            if (class1 == null)
                out.add("");
            else
                out.add(class1.trim());

            if (status == null)
                out.add("");
            else
                out.add(status.trim());

            if (error == null)
                out.add("");
            else
                out.add(error.replaceAll(","," ").trim()); // remove strings

            if (lastUpdate == null)
                out.add("");
            else
                out.add(new SimpleDateFormat("MM/dd/yy HH:mm:ss a z").format(lastUpdate.getTime()));
            out.add(isRequired ? "required": "");
            out.add(Strings.nullToEmpty(source));
            return out.toArray(new String[out.size()]);
        }

        public boolean equals(TreeEntry other) {
            return name.equals(other.getName()) &&
                    type==other.getType() &&
                    fullName.equals(other.getFullName()) &&
                    parent.equals(other.getParent()) &&
                    ((  type==MarketTree.TreeEntry.STOCK &&
                        class1.equals(other.getClass1()) &&
                        status.equals(other.getStatus()) &&
                        (error==null && other.getError()==null || error.equals(other.getError())) &&
                        lastUpdate.equals(other.getLastUpdate())
                    )||
                    (  type==MarketTree.TreeEntry.INDEX &&
                        status.equals(other.getStatus()) &&
                        (error==null && other.getError()==null || error.equals(other.getError())) &&
                        lastUpdate.equals(other.getLastUpdate())
                    )||
                    type==MarketTree.TreeEntry.INDUSTRY);
        }

        public void markStale() {
            status = "stale";
            newRecordTimestamp();
        }

        public void markError(String message) {
            error = message;
            newRecordTimestamp();
        }

        public void markError(Throwable realException) {
            status = "error";
            error = ("Exception: " + realException.toString()+"  ...  Root cause: "+ ExceptionHelper.findRootCause(realException, 50)).replaceAll(","," ").replaceAll("\n"," ").replaceAll("\r", " ");
            if (realException.getClass() == MissingSymbolException.class) {
                MissingSymbolException missing = (MissingSymbolException)realException;
                error = "Missing symbol "+missing.getSymbol()+". Literal error: "+missing.getLiteralError().replaceAll(","," ");
            }
            newRecordTimestamp();
        }

        public void setStatusOk() {
            status = "OK";
            error = "";
            newRecordTimestamp();
        }

        private void newRecordTimestamp() {
            lastUpdate = Calendar.getInstance();
        }

        public boolean isOk() {
            return status.equals("OK");
        }

        public boolean isDisabled() {
            return status.equals("disabled");
        }

        public boolean isDeleted() {
            return status.equals("deleted");
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public String getLoadedFrom() {
        return loadedFrom;
    }


}
