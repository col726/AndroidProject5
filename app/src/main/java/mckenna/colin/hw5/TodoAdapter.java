package mckenna.colin.hw5;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class TodoAdapter extends CursorRecyclerViewAdapter<TodoAdapter.ViewHolder> {
    private Drawable doneDrawable;
    private Drawable dueDrawable;
    private Drawable createdDrawable;
    private Set<Integer> selectedRows = new HashSet<>();

    public interface OnItemClickedListener {
        void onItemClicked(long id);
    }
    private OnItemClickedListener onItemClickedListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView stateView;
        public TextView nameView;
        public TextView descriptionView;
        public TextView priorityView;
        public long id;

        public ViewHolder(View view) {
            super(view);
            stateView = (ImageView) view.findViewById(R.id.state);
            nameView = (TextView) view.findViewById(R.id.name);
            descriptionView = (TextView) view.findViewById(R.id.description);
            priorityView = (TextView) view.findViewById(R.id.priority);
        }
    }

    public TodoAdapter(Context context,
                       Drawable createdDrawable, Drawable dueDrawable, Drawable doneDrawable,
                       OnItemClickedListener onItemClickedListener) {
        super(context, null);
        this.onItemClickedListener = onItemClickedListener;
        this.createdDrawable = createdDrawable;
        this.dueDrawable = dueDrawable;
        this.doneDrawable = doneDrawable;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TodoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.todo_card, parent, false);
        view.setClickable(true);
        final ViewHolder vh = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onItemClickedListener.onItemClicked(vh.id);
            }});
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position, Cursor cursor) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.itemView.setSelected(selectedRows.contains(position));
        TodoItem todoItem = Util.todoItemFromCursor(cursor);
        holder.nameView.setText(todoItem.getName());
        holder.descriptionView.setText(todoItem.getDescription());
        holder.priorityView.setText(todoItem.getPriority() + "");
        holder.id = todoItem.getId();
        switch (todoItem.getState()) {
            case Created:
                holder.stateView.setImageDrawable(createdDrawable);
                break;
            case Due:
                holder.stateView.setImageDrawable(dueDrawable);
                break;
            case Done:
                holder.stateView.setImageDrawable(doneDrawable);
                break;
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        selectedRows.clear();
        return super.swapCursor(newCursor);
    }
}