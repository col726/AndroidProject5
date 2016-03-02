package mckenna.colin.hw5;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class TodoWidget2 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i], Util.getNumDue(context));
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, int due) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_widget2);
        if(due == 0)
            views.setTextViewText(R.id.appwidget_text, "No Items Due");
        else
            views.setTextViewText(R.id.appwidget_text, "Items Due: " + due);


        Intent intent = new Intent(context, TodoListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);

        Intent editIntent = new Intent(context, EditActivity.class);
        PendingIntent editPendingIntent = PendingIntent.getActivity(context, 0, editIntent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_add, editPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

