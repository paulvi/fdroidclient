package org.fdroid.fdroid.data;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.*;

public class AppProvider extends FDroidProvider {

    public static final int MAX_APPS_TO_QUERY = 900;

    public static final class Helper {

        private Helper() {}

        public static int count(Context context, Uri uri) {
            String[] projection = new String[] { AppProvider.DataColumns._COUNT };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            int count = 0;
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
                cursor.close();
            }
            return count;
        }

        public static List<App> all(ContentResolver resolver) {
            return all(resolver, DataColumns.ALL);
        }

        public static List<App> all(ContentResolver resolver, String[] projection) {
            Uri uri = AppProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        public static List<App> findIgnored(Context context, String[] projection) {
            Uri uri = AppProvider.getIgnoredUri();
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        private static List<App> cursorToList(Cursor cursor) {
            int knownAppCount = cursor != null ? cursor.getCount() : 0;
            List<App> apps = new ArrayList<App>(knownAppCount);
            if (cursor != null) {
                if (knownAppCount > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        apps.add(new App(cursor));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            return apps;
        }

        public static String getCategoryAll(Context context) {
            return context.getString(R.string.category_all);
        }

        public static String getCategoryWhatsNew(Context context) {
            return context.getString(R.string.category_whatsnew);
        }

        public static String getCategoryRecentlyUpdated(Context context) {
            return context.getString(R.string.category_recentlyupdated);
        }

        public static List<String> categories(Context context) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri();
            String[] projection = { DataColumns.CATEGORIES };
            Cursor cursor = resolver.query(uri, projection, null, null, null );
            Set<String> categorySet = new HashSet<String>();
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        String categoriesString = cursor.getString(0);
                        if (categoriesString != null) {
                            for (String s : Utils.CommaSeparatedList.make(categoriesString)) {
                                categorySet.add(s);
                            }
                        }
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            List<String> categories = new ArrayList<String>(categorySet);
            Collections.sort(categories);

            // Populate the category list with the real categories, and the
            // locally generated meta-categories for "What's New", "Recently
            // Updated" and "All"...
            categories.add(0, getCategoryAll(context));
            categories.add(0, getCategoryRecentlyUpdated(context));
            categories.add(0, getCategoryWhatsNew(context));

            return categories;
        }

        public static App findById(ContentResolver resolver, String appId) {
            return findById(resolver, appId, DataColumns.ALL);
        }

        public static App findById(ContentResolver resolver, String appId,
                                   String[] projection) {
            Uri uri = getContentUri(appId);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            App app = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    app = new App(cursor);
                }
                cursor.close();
            }
            return app;
        }

        /*
         * I wasn't quite sure on the best way to execute arbitrary queries using the same DBHelper as the
         * content provider class, so I've hidden the implementation of this (by making it private) in case
         * I find a better way in the future.
         */
        public static void calcDetailsFromIndex(Context context) {
            Uri fromUpstream = calcAppDetailsFromIndexUri();
            context.getContentResolver().update(fromUpstream, null, null, null);
        }

    }

    public interface DataColumns {

        public static final String _ID = "rowid as _id"; // Required for CursorLoaders
        public static final String _COUNT = "_count";
        public static final String IS_COMPATIBLE = "compatible";
        public static final String APP_ID = "id";
        public static final String NAME = "name";
        public static final String SUMMARY = "summary";
        public static final String ICON = "icon";
        public static final String DESCRIPTION = "description";
        public static final String LICENSE = "license";
        public static final String WEB_URL = "webURL";
        public static final String TRACKER_URL = "trackerURL";
        public static final String SOURCE_URL = "sourceURL";
        public static final String DONATE_URL = "donateURL";
        public static final String BITCOIN_ADDR = "bitcoinAddr";
        public static final String LITECOIN_ADDR = "litecoinAddr";
        public static final String DOGECOIN_ADDR = "dogecoinAddr";
        public static final String FLATTR_ID = "flattrID";
        public static final String SUGGESTED_VERSION_CODE = "suggestedVercode";
        public static final String UPSTREAM_VERSION = "upstreamVersion";
        public static final String UPSTREAM_VERSION_CODE = "upstreamVercode";
        public static final String ADDED = "added";
        public static final String LAST_UPDATED = "lastUpdated";
        public static final String CATEGORIES = "categories";
        public static final String ANTI_FEATURES = "antiFeatures";
        public static final String REQUIREMENTS = "requirements";
        public static final String IGNORE_ALLUPDATES = "ignoreAllUpdates";
        public static final String IGNORE_THISUPDATE = "ignoreThisUpdate";
        public static final String ICON_URL = "iconUrl";

        public interface SuggestedApk {
            public static final String VERSION = "suggestedApkVersion";
        }

        public interface InstalledApp {
            public static final String VERSION_CODE = "installedVersionCode";
            public static final String VERSION_NAME = "installedVersionName";
        }

        public static String[] ALL = {
                IS_COMPATIBLE, APP_ID, NAME, SUMMARY, ICON, DESCRIPTION,
                LICENSE, WEB_URL, TRACKER_URL, SOURCE_URL, DONATE_URL,
                BITCOIN_ADDR, LITECOIN_ADDR, DOGECOIN_ADDR, FLATTR_ID,
                UPSTREAM_VERSION, UPSTREAM_VERSION_CODE, ADDED, LAST_UPDATED,
                CATEGORIES, ANTI_FEATURES, REQUIREMENTS, IGNORE_ALLUPDATES,
                IGNORE_THISUPDATE, ICON_URL, SUGGESTED_VERSION_CODE,
                SuggestedApk.VERSION, InstalledApp.VERSION_CODE,
                InstalledApp.VERSION_NAME
        };
    }

    /**
     * A QuerySelection which is aware of the option/need to join onto the
     * installed apps table. Not that the base classes
     * {@link org.fdroid.fdroid.data.QuerySelection#add(QuerySelection)} and
     * {@link org.fdroid.fdroid.data.QuerySelection#add(String, String[])} methods
     * will only return the base class {@link org.fdroid.fdroid.data.QuerySelection}
     * which is not aware of the installed app table.
     * However, the
     * {@link org.fdroid.fdroid.data.AppProvider.AppQuerySelection#add(org.fdroid.fdroid.data.AppProvider.AppQuerySelection)}
     * method from this class will return an instance of this class, that is aware of
     * the install apps table.
     */
    private static class AppQuerySelection extends QuerySelection {

        private boolean naturalJoinToInstalled = false;

        public AppQuerySelection() {
            // The same as no selection, because "1" will always resolve to true when executing the SQL query.
            // e.g. "WHERE 1 AND ..." is the same as "WHERE ..."
            super("1");
        }

        public AppQuerySelection(String selection) {
            super(selection);
        }

        public AppQuerySelection(String selection, String[] args) {
            super(selection, args);
        }

        public AppQuerySelection(String selection, List<String> args) {
            super(selection, args);
        }

        public boolean naturalJoinToInstalled() {
            return naturalJoinToInstalled;
        }

        /**
         * Tells the query selection that it will need to join onto the installed apps table
         * when used. This should be called when your query makes use of fields from that table
         * (for example, list all installed, or list those which can be updated).
         * @return A reference to this object, to allow method chaining, for example
         * <code>return new AppQuerySelection(selection).requiresInstalledTable())</code>
         */
        public AppQuerySelection requireNaturalInstalledTable() {
            naturalJoinToInstalled = true;
            return this;
        }

        public AppQuerySelection add(AppQuerySelection query) {
            QuerySelection both = super.add(query);
            AppQuerySelection bothWithJoin = new AppQuerySelection(both.getSelection(), both.getArgs());
            if (this.naturalJoinToInstalled() || query.naturalJoinToInstalled()) {
                bothWithJoin.requireNaturalInstalledTable();
            }
            return bothWithJoin;
        }

    }

    private static class Query extends QueryBuilder {

        private boolean isSuggestedApkTableAdded = false;
        private boolean requiresInstalledTable = false;
        private boolean categoryFieldAdded = false;

        @Override
        protected String getRequiredTables() {
            return DBHelper.TABLE_APP;
        }

        @Override
        protected boolean isDistinct() {
            return fieldCount() == 1 && categoryFieldAdded;
        }

        public void addSelection(AppQuerySelection selection) {
            addSelection(selection.getSelection());
            if (selection.naturalJoinToInstalled()) {
                naturalJoinToInstalledTable();
            }
        }

        // TODO: What if the selection requires a natural join, but we first get a left join
        // because something causes leftJoin to be caused first? Maybe throw an exception?
        public void naturalJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                join(
                    DBHelper.TABLE_INSTALLED_APP,
                    "installed",
                    "installed." + InstalledAppProvider.DataColumns.APP_ID + " = " + DBHelper.TABLE_APP + ".id");
                requiresInstalledTable = true;
            }
        }

        public void leftJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                leftJoin(
                    DBHelper.TABLE_INSTALLED_APP,
                    "installed",
                    "installed." + InstalledAppProvider.DataColumns.APP_ID + " = " + DBHelper.TABLE_APP + ".id");
                requiresInstalledTable = true;
            }
        }

        @Override
        public void addField(String field) {
            if (field.equals(DataColumns.SuggestedApk.VERSION)) {
                addSuggestedApkVersionField();
            } else if (field.equals(DataColumns.InstalledApp.VERSION_NAME)) {
                addInstalledAppVersionName();
            } else if (field.equals(DataColumns.InstalledApp.VERSION_CODE)) {
                addInstalledAppVersionCode();
            } else if (field.equals(DataColumns._COUNT)) {
                appendCountField();
            } else {
                if (field.equals(DataColumns.CATEGORIES)) {
                    categoryFieldAdded = true;
                }
                appendField(field, "fdroid_app");
            }
        }

        private void appendCountField() {
            appendField("COUNT(*) AS " + DataColumns._COUNT);
        }

        private void addSuggestedApkVersionField() {
            addSuggestedApkField(
                    ApkProvider.DataColumns.VERSION,
                    DataColumns.SuggestedApk.VERSION);
        }

        private void addSuggestedApkField(String fieldName, String alias) {
            if (!isSuggestedApkTableAdded) {
                isSuggestedApkTableAdded = true;
                leftJoin(
                    DBHelper.TABLE_APK,
                    "suggestedApk",
                    "fdroid_app.suggestedVercode = suggestedApk.vercode AND fdroid_app.id = suggestedApk.id");
            }
            appendField(fieldName, "suggestedApk", alias);
        }

        private void addInstalledAppVersionName() {
            addInstalledAppField(
                    InstalledAppProvider.DataColumns.VERSION_NAME,
                    DataColumns.InstalledApp.VERSION_NAME
            );
        }

        private void addInstalledAppVersionCode() {
            addInstalledAppField(
                    InstalledAppProvider.DataColumns.VERSION_CODE,
                    DataColumns.InstalledApp.VERSION_CODE
            );
        }

        private void addInstalledAppField(String fieldName, String alias) {
            leftJoinToInstalledTable();
            appendField(fieldName, "installed", alias);
        }
    }

    private static final String PROVIDER_NAME = "AppProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    private static final String PATH_INSTALLED = "installed";
    private static final String PATH_CAN_UPDATE = "canUpdate";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_NO_APKS = "noApks";
    private static final String PATH_APPS = "apps";
    private static final String PATH_RECENTLY_UPDATED = "recentlyUpdated";
    private static final String PATH_NEWLY_ADDED = "newlyAdded";
    private static final String PATH_CATEGORY = "category";
    private static final String PATH_IGNORED = "ignored";
    private static final String PATH_CALC_APP_DETAILS_FROM_INDEX = "calcDetailsFromIndex";

    private static final int CAN_UPDATE       = CODE_SINGLE + 1;
    private static final int INSTALLED        = CAN_UPDATE + 1;
    private static final int SEARCH           = INSTALLED + 1;
    private static final int NO_APKS          = SEARCH + 1;
    private static final int APPS             = NO_APKS + 1;
    private static final int RECENTLY_UPDATED = APPS + 1;
    private static final int NEWLY_ADDED      = RECENTLY_UPDATED + 1;
    private static final int CATEGORY         = NEWLY_ADDED + 1;
    private static final int IGNORED          = CATEGORY + 1;
    private static final int CALC_APP_DETAILS_FROM_INDEX = IGNORED + 1;

    static {
        matcher.addURI(getAuthority(), null, CODE_LIST);
        matcher.addURI(getAuthority(), PATH_CALC_APP_DETAILS_FROM_INDEX, CALC_APP_DETAILS_FROM_INDEX);
        matcher.addURI(getAuthority(), PATH_IGNORED, IGNORED);
        matcher.addURI(getAuthority(), PATH_RECENTLY_UPDATED, RECENTLY_UPDATED);
        matcher.addURI(getAuthority(), PATH_NEWLY_ADDED, NEWLY_ADDED);
        matcher.addURI(getAuthority(), PATH_CATEGORY + "/*", CATEGORY);
        matcher.addURI(getAuthority(), PATH_SEARCH + "/*", SEARCH);
        matcher.addURI(getAuthority(), PATH_CAN_UPDATE, CAN_UPDATE);
        matcher.addURI(getAuthority(), PATH_INSTALLED, INSTALLED);
        matcher.addURI(getAuthority(), PATH_NO_APKS, NO_APKS);
        matcher.addURI(getAuthority(), PATH_APPS + "/*", APPS);
        matcher.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getRecentlyUpdatedUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_RECENTLY_UPDATED);
    }

    public static Uri getNewlyAddedUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_NEWLY_ADDED);
    }

    public static Uri getIgnoredUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_IGNORED);
    }

    private static Uri calcAppDetailsFromIndexUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_CALC_APP_DETAILS_FROM_INDEX);
    }

    public static Uri getCategoryUri(String category) {
        return getContentUri().buildUpon()
            .appendPath(PATH_CATEGORY)
            .appendPath(category)
            .build();
    }

    public static Uri getNoApksUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_NO_APKS);
    }

    public static Uri getInstalledUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_INSTALLED);
    }

    public static Uri getCanUpdateUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_CAN_UPDATE);
    }

    public static Uri getContentUri(List<App> apps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < apps.size(); i ++) {
            if (i != 0) {
                builder.append(',');
            }
            builder.append(apps.get(i).id);
        }
        return getContentUri().buildUpon()
            .appendPath(PATH_APPS)
            .appendPath(builder.toString())
            .build();
    }

    public static Uri getContentUri(App app) {
        return getContentUri(app.id);
    }

    public static Uri getContentUri(String appId) {
        return Uri.withAppendedPath(getContentUri(), appId);
    }

    public static Uri getSearchUri(String query) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH)
            .appendPath(query)
            .build();
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_APP;
    }

    @Override
    protected String getProviderName() {
        return "AppProvider";
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return matcher;
    }

    private AppQuerySelection queryCanUpdate() {
        String ignoreCurrent = " fdroid_app.ignoreThisUpdate != fdroid_app.suggestedVercode ";
        String ignoreAll = " fdroid_app.ignoreAllUpdates != 1 ";
        String ignore = " ( " + ignoreCurrent + " AND " + ignoreAll + " ) ";
        String where = ignore + " AND fdroid_app." + DataColumns.SUGGESTED_VERSION_CODE + " > installed.versionCode";
        return new AppQuerySelection(where).requireNaturalInstalledTable();
    }

    private AppQuerySelection queryInstalled() {
        return new AppQuerySelection().requireNaturalInstalledTable();
    }

    private AppQuerySelection querySearch(String keywords) {
        keywords = "%" + keywords + "%";
        String selection =
                "fdroid_app.id like ? OR " +
                "fdroid_app.name like ? OR " +
                "fdroid_app.summary like ? OR " +
                "fdroid_app.description like ? ";
        String[] args = new String[] { keywords, keywords, keywords, keywords};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection querySingle(String id) {
        String selection = "fdroid_app.id = ?";
        String[] args = { id };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryIgnored() {
        String selection = "fdroid_app.ignoreAllUpdates = 1 OR " +
                "fdroid_app.ignoreThisUpdate >= fdroid_app.suggestedVercode";
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryNewlyAdded() {
        String selection = "fdroid_app.added > ?";
        String[] args = { Utils.DATE_FORMAT.format(Preferences.get().calcMaxHistory()) };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryRecentlyUpdated() {
        String selection = "fdroid_app.added != fdroid_app.lastUpdated AND fdroid_app.lastUpdated > ?";
        String[] args = { Utils.DATE_FORMAT.format(Preferences.get().calcMaxHistory()) };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryCategory(String category) {
        // TODO: In the future, add a new table for categories,
        // so we can join onto it.
        String selection =
                " fdroid_app.categories = ? OR " +    // Only category e.g. "internet"
                " fdroid_app.categories LIKE ? OR " + // First category e.g. "internet,%"
                " fdroid_app.categories LIKE ? OR " + // Last category e.g. "%,internet"
                " fdroid_app.categories LIKE ? ";     // One of many categories e.g. "%,internet,%"
        String[] args = {
                category,
                category + ",%",
                "%," + category,
                "%," + category + ",%",
        };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryNoApks() {
        String selection = "(SELECT COUNT(*) FROM fdroid_apk WHERE fdroid_apk.id = fdroid_app.id) = 0";
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryApps(String appIds) {
        String[] args = appIds.split(",");
        String selection = "fdroid_app.id IN (" + generateQuestionMarksForInClause(args.length) + ")";
        return new AppQuerySelection(selection, args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        Query query = new Query();
        AppQuerySelection selection = new AppQuerySelection(customSelection, selectionArgs);
        switch (matcher.match(uri)) {
            case CODE_LIST:
                break;

            case CODE_SINGLE:
                selection = selection.add(querySingle(uri.getLastPathSegment()));
                break;

            case CAN_UPDATE:
                selection = selection.add(queryCanUpdate());
                break;

            case INSTALLED:
                selection = selection.add(queryInstalled());
                break;

            case SEARCH:
                selection = selection.add(querySearch(uri.getLastPathSegment()));
                break;

            case NO_APKS:
                selection = selection.add(queryNoApks());
                break;

            case APPS:
                selection = selection.add(queryApps(uri.getLastPathSegment()));
                break;

            case IGNORED:
                selection = selection.add(queryIgnored());
                break;

            case CATEGORY:
                selection = selection.add(queryCategory(uri.getLastPathSegment()));
                break;

            case RECENTLY_UPDATED:
                sortOrder = " fdroid_app.lastUpdated DESC";
                selection = selection.add(queryRecentlyUpdated());
                break;

            case NEWLY_ADDED:
                sortOrder = " fdroid_app.added DESC";
                selection = selection.add(queryNewlyAdded());
                break;

            default:
                Log.e("FDroid", "Invalid URI for app content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
        }

        if (AppProvider.DataColumns.NAME.equals(sortOrder)) {
            sortOrder = " lower( fdroid_app." + sortOrder + " ) ";
        }

        query.addSelection(selection);
        query.addFields(projection); // TODO: Make the order of addFields/addSelection not dependent on each other...
        query.addOrderBy(sortOrder);

        Cursor cursor = read().rawQuery(query.toString(), selection.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {

            case NO_APKS:
                query = query.add(queryNoApks());
                break;

            default:
                throw new UnsupportedOperationException("Delete not supported for " + uri + ".");

        }

        int count = write().delete(getTableName(), query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        write().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getContentUri(values.getAsString(DataColumns.APP_ID));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {

            case CALC_APP_DETAILS_FROM_INDEX:
                updateAppDetails();
                return 0;

            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            default:
                throw new UnsupportedOperationException("Update not supported for " + uri + ".");

        }
        int count = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private void updateAppDetails() {
        updateCompatibleFlags();
        updateSuggestedFromLatest();
        updateSuggestedFromUpstream();
        updateIconUrls();
    }

    /**
     * Look at the upstream version of each app, our goal is to find the apk
     * with the closest version code to that, without going over.
     * If the app is not compatible at all (i.e. no versions were compatible)
     * then we take the highest, otherwise we take the highest compatible version.
     *
     * Replaces the existing Java code:
     *
     * if (app.upstreamVercode > 0) {
     *     int latestcode = -1;
     *     for (Apk apk : apksForApp) {
     *         if ((!app.compatible || apk.compatible)
     *                 && apk.vercode <= app.upstreamVercode
     *                 && apk.vercode > latestcode) {
     *             latestApk = apk;
     *             latestcode = apk.vercode;
     *         }
     *     }
     * }
     *
     * And it can be read a little easier like this (without the string concats):
     *
     *   UPDATE fdroid_app
     *   SET suggestedVercode = (
     *       SELECT MAX(fdroid_apk.vercode)
     *       FROM fdroid_apk
     *       WHERE
     *           fdroid_app.id = fdroid_apk.id AND
     *           fdroid_apk.vercode <= fdroid_app.upstreamVercode AND
     *           ( fdroid_app.compatible = 0 OR fdroid_apk.compatible = 1 )
     *   )
     *   WHERE upstreamVercode > 0
     */
    private void updateSuggestedFromUpstream() {

        Log.d("FDroid", "Calculating suggested versions for all apps which specify an upstream version code.");

        final String apk = DBHelper.TABLE_APK;
        final String app = DBHelper.TABLE_APP;

        String updateSql =
            "UPDATE " + app +
            " SET suggestedVercode = ( " +
                " SELECT MAX( " + apk + ".vercode ) " +
                " FROM " + apk +
                " WHERE " +
                    app + ".id = " + apk + ".id AND " +
                    apk + ".vercode <= " + app + ".upstreamVercode AND " +
                    " ( " + app + ".compatible = 0 OR " + apk + ".compatible = 1 ) ) " +
            " WHERE upstreamVercode > 0 ";

        write().execSQL(updateSql);
    }

    /**
     * For each app, we want to set the isCompatible flag to 1 if any of the apks we know
     * about are compatible, and 0 otherwise.
     *
     * Here is the SQL query without all of the concatenations (hopefully it's a bit easier to read):
     *
     *  UPDATE fdroid_app SET compatible = (
     *      SELECT TOTAL( fdroid_apk.compatible ) > 0
     *      FROM fdroid_apk
     *      WHERE fdroid_apk.id = fdroid_app.id );
     */
    private void updateCompatibleFlags() {

        Log.d("FDroid", "Calculating whether apps are compatible, based on whether any of their apks are compatible");

        final String apk = DBHelper.TABLE_APK;
        final String app = DBHelper.TABLE_APP;

        String updateSql =
            "UPDATE " + app + " SET compatible = ( " +
                " SELECT TOTAL( " + apk + ".compatible ) > 0 " +
                " FROM " + apk +
                " WHERE " + apk + ".id = " + app + ".id );";

        write().execSQL(updateSql);
    }

    /**
     * For all apps that don't specify an upstream version code, we take the
     * latest apk in the repo. If the app is not compatible at all (i.e. no versions
     * were compatible) then we take the highest, otherwise we take the highest
     * compatible version.
     *
     * Replaces the existing Java code:
     *
     * for (Apk apk : apksForApp) {
     *     if ((!app.compatible || apk.compatible)
     *             && apk.vercode > latestCode) {
     *         latestApk = apk;
     *         latestCode = apk.vercode;
     *     }
     * }
     *
     * And it can be read a little easier like this (without the string concats):
     *
     *  UPDATE fdroid_app
     *  SET suggestedVercode = (
     *      SELECT MAX(fdroid_apk.vercode)
     *      FROM fdroid_apk
     *      WHERE
     *          fdroid_app.id = fdroid_apk.id AND
     *          ( fdroid_app.compatible = 0 OR fdroid_apk.compatible = 1 )
     *  )
     *  WHERE upstreamVercode = 0 OR upstreamVercode IS NULL;
     */
    private void updateSuggestedFromLatest() {

        Log.d("FDroid", "Calculating suggested versions for all apps which don't specify an upstream version code.");

        final String apk = DBHelper.TABLE_APK;
        final String app = DBHelper.TABLE_APP;

        String updateSql =
            "UPDATE " + app +
            " SET suggestedVercode = ( " +
                " SELECT MAX( " + apk + ".vercode ) " +
                " FROM " + apk +
                " WHERE " +
                    app + ".id = " + apk + ".id AND " +
                    " ( " + app + ".compatible = 0 OR " + apk + ".compatible = 1 ) ) " +
            " WHERE upstreamVercode = 0 OR upstreamVercode IS NULL ";

        write().execSQL(updateSql);
    }

    private void updateIconUrls() {

        Log.d("FDroid", "Updating icon paths for apps belonging to repos with version >= " + Repo.VERSION_DENSITY_SPECIFIC_ICONS);
        String iconsDir = Utils.getIconsDir(getContext());
        Log.d("FDroid", "Using icon dir '"+iconsDir+"'");
        String repoVersion = Integer.toString(Repo.VERSION_DENSITY_SPECIFIC_ICONS);
        String query = getIconUpdateQuery();
        String[] params = { repoVersion, iconsDir };
        write().execSQL(query, params);
    }

    /**
     * Returns a query which requires two parameters to be bound. These are (in order):
     *  1) The repo version that introduced density specific icons
     *  2) The dir to density specific icons for the current device.
     */
    private String getIconUpdateQuery() {

        final String apk = DBHelper.TABLE_APK;
        final String app = DBHelper.TABLE_APP;
        final String repo = DBHelper.TABLE_REPO;

        return
            " UPDATE " + app + " SET iconUrl = ( " +
                " SELECT " +

                    // Concatenate (using the "||" operator) the address, the icons directory (bound to the ? as the
                    // second parameter when executing the query) and the icon path.
                    " ( " +
                        repo + ".address " +
                        " || " +

                        // If the repo has the relevant version, then use a more intelligent icons dir,
                        // otherwise revert to '/icons/'
                        " CASE WHEN " + repo + ".version >= ? THEN ? ELSE '/icons/' END " +

                        " || " +
                        app + ".icon " +
                    ") " +
                " FROM " +
                    apk +
                    " JOIN " + repo + " ON (" + repo + "._id = " + apk + ".repo) " +
                " WHERE " +
                    app + ".id = " + apk + ".id AND " +
                    apk + ".vercode = ( " +

                        // We only want the latest apk here. Ideally, we should instead join
                        // onto apk.suggestedVercode, but as per https://gitlab.com/fdroid/fdroidclient/issues/1
                        // there may be some situations where suggestedVercode isn't set.
                        // TODO: If we can guarantee that suggestedVercode is set, then join onto that instead.
                        // This will save from doing a futher sub query for each app.
                        " SELECT MAX(inner_apk.vercode)  " +
                        " FROM fdroid_apk as inner_apk " +
                        " WHERE inner_apk.id = fdroid_apk.id ) " +
                    " AND fdroid_apk.repo = fdroid_repo._id " +
            " ) ";
    }

}
