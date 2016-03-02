package mckenna.colin.hw5;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import java.util.ArrayList;

public class TodoProvider extends ContentProvider {
    // Database Constants
    private static final String TODO_TABLE = "todo";        // table name
    public static final String ID = "_id";                  // ID column - NOTE THE UNDERSCORE!
    public static final String NAME = "name";               // name column
    public static final String DESCRIPTION = "description"; // description column
    public static final String DONE = "done";               // done column - added in version 2
    public static final String PRIORITY = "priority";       // priority column added in version 3
    public static final String STATE = "state";             // priority column added in version 4
    public static final int DB_VERSION = 4;                 // current database version

    // URI Constants
    public static final int TODOS = 1;
    public static final int TODO_ITEM = 2;
    public static final String AUTHORITY = "mckenna.colin.hw5";
    public static final String BASE_PATH = "todo";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.javadude.todo";
    public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.javadude.todo";
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH, TODOS);
        // if we see content://com.javadude.todo/todo -> return TODOS (1)
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH + "/#", TODO_ITEM);
        // if we see content://com.javadude.todo/todo/42 -> return TODO_ITEM (2)
    }

    //Notification Constant
    private static final int NOTIFICATION_ID = 1;


    // database "open helper" class
    // when asked to open a database, the base SQLLiteOpenHelper
    //   will check if the database exists - if not, onCreate will be called
    // if the database exists, it checks if the current version on disk matches
    //   the version specified in the constructor - if not, onUpgrade will be called
    // then the database is opened and returned to the caller
    private static class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context) {
            super(context, "TODO", null, DB_VERSION);
        }

        // create the database if it doesn't exist
        @Override public void onCreate(SQLiteDatabase db) {
            try {
                db.beginTransaction();
                // always keep version 1 creation
                String sql = String.format(
                        "create table %s (%s integer primary key autoincrement, %s text, %s text)",
                        TODO_TABLE, ID, NAME, DESCRIPTION);
                db.execSQL(sql);
                onUpgrade(db, 1, DB_VERSION);  // run the upgrades starting from version 1
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        // run upgrade logic to convert from one version to a later version
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // UPGRADE REALLY SHOULD SAVE EXISTING DATA IF AT ALL POSSIBLE
            //   - use "alter table" where possible
            // The following example assumes we're up to version 3

            try {
                db.beginTransaction();
                switch(oldVersion) {
                    default:
                        throw new IllegalStateException("Unexpected existing database version " + oldVersion);

                    case 1:
                        // do upgrades from version 1 -> version 2
                        db.execSQL(String.format("alter table %s add %s integer", TODO_TABLE, DONE));
                        // FALL THROUGH TO APPLY FURTHER UPGRADES
                    case 2:
                        // do upgrades from version 2 -> version 3
                        db.execSQL(String.format("alter table %s add %s text", TODO_TABLE, PRIORITY));
                        // FALL THROUGH TO APPLY FURTHER UPGRADES
                    case 3:
                        // do upgrades from version 3 -> version 4
                        db.execSQL(String.format("alter table %s add %s text", TODO_TABLE, STATE));
                        // FALL THROUGH TO APPLY FURTHER UPGRADES
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        db = new OpenHelper(getContext()).getWritableDatabase();
        return true; // data source opened ok!
    }

    // retrieve data from the underlying data store
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // uri - the base request from the caller
        // projection - which columns the caller wants to retrieve
        // selection - the "where" clause for the query (without the "where") - usually should have "?" for parameters
        // selectionArgs - the values to use when filling in the "?"
        // sortOrder - the "orderby" clause (without the "orderby")

        // behave a little differently depending on the type of URI we find
        // ask the URI_MATCHER to parse the URI and tell us what it looks like
        switch (URI_MATCHER.match(uri)) {
            // if the URI looks like content://com.javadude.todo/todo (all todos)
            case TODOS: {
                // get all TODOS from the database
                Cursor c = db.query(TODO_TABLE,
                        projection,
                        selection,
                        selectionArgs,
                        null, null, // groupby, having
                        sortOrder);

                // Set the notification URI on the returned cursor so it can listen for changes that we might make!
                // When updates are made, we indicate the affected URIs and all cursors registered like this are notified
                // This makes ListView updates completely automatic (as we'll see later)
                c.setNotificationUri(getContext().getContentResolver(), uri);
                return c;
            }

            // if the uri looks like content://com.javadude.todo/todo/42 (single todo item w/id)
            case TODO_ITEM: {
                // get specific item
                String id = uri.getLastPathSegment(); // what's the id?
                Cursor c = db.query(TODO_TABLE,
                        projection,
                        ID + "=?", // DO NOT simply say ID + "=" + id! SQL INJECTION!
                        new String[] {id},
                        null, null, // groupby, having
                        sortOrder);
                // Set the notification URI on the returned cursor so it can listen for changes that we might make!
                // When updates are made, we indicate the affected URIs and all cursors registered like this are notified
                // This makes ListView updates completely automatic (as we'll see later)
                c.setNotificationUri(getContext().getContentResolver(), uri);
                return c;
            }
            default:
                return null; // unknown
        }
    }

    // allows callers to find out what kind of data will be returned by a given URI
    // similar to the logic we have in query(), but just returns a MIME type representing the data for the URI
    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case TODOS:
                return CONTENT_DIR_TYPE;
            case TODO_ITEM:
                return CONTENT_ITEM_TYPE;
            default:
                return null; // unknown
        }
    }

    // insert a new row into the data store
    @Override public Uri insert(Uri uri, ContentValues values) {
        // uri is a parent container to which we are adding
        // values is a map of column/value entries for the row

        // do the actual insert into the database
        long id = db.insert(TODO_TABLE, null, values);
        // the null is the "null column hack"
        //   -- if inserting an empty row, specify the name of a nullable column

        // send a notification that the data has changed
        // note that we pass the parent container (which is the one being observed)
        getContext().getContentResolver().notifyChange(uri, null);
        createNotification(NOTIFICATION_ID);
        updateWidget();
        // return the URI of the inserted data so we can refer to it later
        return Uri.withAppendedPath(CONTENT_URI, ""+id);
    }

    // delete from the data store
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // selection is a "where" clause (without the "where")
        // selectionArgs replaces the ? in the selection to avoid SQL injection

        int numDeleted = 0;
        switch (URI_MATCHER.match(uri)) {
            // if the URI looks like content://com.javadude.todo/todo (all todos)
            case TODOS: {
                numDeleted = db.delete(TODO_TABLE, selection, selectionArgs);
                break;
            }

            // if the uri looks like content://com.javadude.todo/todo/42 (single todo item w/id)
            case TODO_ITEM: {
                // get specific item
                String id = uri.getLastPathSegment(); // what's the id?
                numDeleted = db.delete(TODO_TABLE, ID + "=?", new String[] {id});
                break;
            }
        }

        // send a notification that the data has changed
        // this will notify cursors that were registered for the container URI
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        createNotification(NOTIFICATION_ID);
        updateWidget();
        return numDeleted;
    }

    // update the data store
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // values are the values to update
        // selection is a "where" clause, useful if updating multiple items
        // selectionArgs replaces the "?"s in the selection

        int numUpdated = 0;
        switch (URI_MATCHER.match(uri)) {
            // if the URI looks like content://com.javadude.todo/todo (all todos)
            case TODOS: {
                numUpdated = db.update(TODO_TABLE, values, selection, selectionArgs);
                break;
            }

            // if the uri looks like content://com.javadude.todo/todo/42 (single todo item w/id)
            case TODO_ITEM: {
                // get specific item
                String id = uri.getLastPathSegment(); // what's the id?
                numUpdated = db.update(TODO_TABLE, values, ID + "=?", new String[]{id});
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
        }

        // send a notification that the data has changed
        // this will notify cursors that were registered for the container URI
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        createNotification(NOTIFICATION_ID);
        updateWidget();
        return numUpdated;
    }

    private void updateWidget() {

        ComponentName thisWidget = new ComponentName(getContext(), TodoWidget2.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for(int i = 0; i < allWidgetIds.length; i++) {
            TodoWidget2.updateAppWidget(getContext(), appWidgetManager, allWidgetIds[i], Util.getNumDue(getContext()));
        }
    }

    private PendingIntent createPending(int requestId) {
        Intent detailIntent = new Intent(getContext(), EditActivity.class);
        detailIntent.putExtra("data", -1);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
        stackBuilder.addParentStack(TodoListActivity.class);
        stackBuilder.addNextIntent(detailIntent);
        return stackBuilder.getPendingIntent(requestId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

   private void createNotification(int n) {

       NotificationManager notificationManager =
               (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
       ArrayList<String> dueItems = new ArrayList<>();

       //get string array with all due items
       String[] projection = {
               TodoProvider.NAME
       };

       String[] selectionArgs = {"Due"};
       Cursor due = query(CONTENT_URI, projection, STATE + "=?", selectionArgs, null);

       if(due.getCount() == 0)
       {
           notificationManager.cancel(n);
       }
       else {
           while (due.moveToNext()) {
               dueItems.add(due.getString(0));
           }

           PendingIntent detailPendingIntent = createPending(n);

           NotificationCompat.InboxStyle inboxStyle =
                   new NotificationCompat.InboxStyle()
                           .setBigContentTitle("Items Due!!!")
                           .setSummaryText("Total Items Due: ");

           for (String s : dueItems) {
               inboxStyle.addLine(s);
           }

           //PendingIntent detailPendingIntent = createPending(n, n, "Details");

           Bitmap icon = BitmapFactory.decodeResource(getContext().getResources(), android.R.drawable.ic_menu_gallery);
           Notification notification =
                   new NotificationCompat.Builder(getContext())
                           .setNumber(dueItems.size())
                           .setStyle(inboxStyle)
                           .setTicker("You have " + dueItems.size() + " items due!")
                           .setLargeIcon(icon)
                           .setAutoCancel(true)
                           .setOngoing(true)
                           .setSmallIcon(R.drawable.ic_items_due)
                           .setContentIntent(detailPendingIntent)
                                   // .addAction(R.drawable.ic_play_arrow_24dp, "Play", playPendingIntent)
                                   //.addAction(R.drawable.ic_stop_24dp, "Stop", stopPendingIntent)
                           .build();


           notificationManager.notify(n, notification);
       }

    }
}
