// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.fm;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.fm.icons.FmIconFetcher;
import io.github.muntashirakon.AppManager.self.imagecache.ImageLoader;
import io.github.muntashirakon.AppManager.utils.DateUtils;
import io.github.muntashirakon.AppManager.utils.TextUtilsCompat;
import io.github.muntashirakon.AppManager.utils.UIUtils;
import io.github.muntashirakon.AppManager.utils.appearance.ColorCodes;
import io.github.muntashirakon.widget.MultiSelectionView;

class FmAdapter extends MultiSelectionView.Adapter<FmAdapter.ViewHolder> {
    private static final List<String> DEX_EXTENSIONS = Arrays.asList("dex", "jar");

    private final List<FmItem> mAdapterList = Collections.synchronizedList(new ArrayList<>());
    private final FmViewModel mViewModel;
    private final FmActivity mFmActivity;
    @ColorInt
    private final int mHighlightColor;

    public FmAdapter(FmViewModel viewModel, FmActivity activity) {
        mViewModel = viewModel;
        mFmActivity = activity;
        mHighlightColor = ColorCodes.getListItemSelectionColor(activity);
    }

    public void setFmList(List<FmItem> list) {
        mAdapterList.clear();
        mAdapterList.addAll(list);
        notifySelectionChange();
        notifyDataSetChanged();
    }

    @Override
    public int getHighlightColor() {
        return mHighlightColor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fm, parent, false);
        View actionView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_right_standalone_action, parent, false);
        LinearLayoutCompat layout = view.findViewById(android.R.id.widget_frame);
        layout.addView(actionView);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FmItem item = mAdapterList.get(position);
        holder.title.setText(item.path.getName());
        String modificationDate = DateUtils.formatDateTime(mFmActivity, item.path.lastModified());
        // Set icon
        ImageLoader.getInstance().displayImage(item.tag, holder.icon, new FmIconFetcher(item));
        // Set sub-icon
        // TODO: 24/5/23 Set sub-icon if needed
        if (item.type == FileType.DIRECTORY) {
            holder.subtitle.setText(String.format(Locale.getDefault(), "%d • %s", item.path.listFiles().length,
                    modificationDate));
            holder.itemView.setOnClickListener(v -> mViewModel.loadFiles(item.path.getUri()));
        } else {
            holder.subtitle.setText(String.format(Locale.getDefault(), "%s • %s",
                    Formatter.formatShortFileSize(mFmActivity, item.path.length()), modificationDate));
            holder.itemView.setOnClickListener(v -> {
                // TODO: 16/11/22 Retrieve default open with from DB and open the file with it
                OpenWithDialogFragment fragment = OpenWithDialogFragment.getInstance(item.path);
                fragment.show(mFmActivity.getSupportFragmentManager(), OpenWithDialogFragment.TAG);
            });
        }
        // Symbolic link
        holder.symbolicLinkIcon.setVisibility(item.path.isSymbolicLink() ? View.VISIBLE : View.GONE);
        // Set background colors
        holder.itemView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
        // Set selections
        holder.icon.setOnClickListener(v -> toggleSelection(position));
        // Set actions
        holder.action.setOnClickListener(v -> displayActions(holder.action, item));
        super.onBindViewHolder(holder, position);
    }

    @Override
    public long getItemId(int position) {
        return mAdapterList.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return mAdapterList.size();
    }

    @Override
    protected void select(int position) {
        // TODO: 4/7/21
    }

    @Override
    protected void deselect(int position) {
        // TODO: 4/7/21
    }

    @Override
    protected boolean isSelected(int position) {
        // TODO: 4/7/21
        return false;
    }

    @Override
    protected void cancelSelection() {
        super.cancelSelection();
        // TODO: 4/7/21
    }

    @Override
    protected int getSelectedItemCount() {
        // TODO: 4/7/21
        return 0;
    }

    @Override
    protected int getTotalItemCount() {
        return mAdapterList.size();
    }

    private void displayActions(View anchor, FmItem item) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.inflate(R.menu.fragment_fm_item_actions);
        Menu menu = popupMenu.getMenu();
        MenuItem openWithAction = menu.findItem(R.id.action_open_with);
        MenuItem cutAction = menu.findItem(R.id.action_cut);
        MenuItem copyAction = menu.findItem(R.id.action_copy);
        MenuItem renameAction = menu.findItem(R.id.action_rename);
        MenuItem deleteAction = menu.findItem(R.id.action_delete);
        MenuItem shareAction = menu.findItem(R.id.action_share);
        // Disable actions based on criteria
        boolean canRead = item.path.canRead();
        boolean canWrite = item.path.canWrite();
        openWithAction.setEnabled(canRead);
        cutAction.setEnabled(canRead && canWrite);
        copyAction.setEnabled(canRead);
        renameAction.setEnabled(canRead && canWrite);
        deleteAction.setEnabled(canRead && canWrite);
        shareAction.setEnabled(canRead);
        // Set actions
        openWithAction.setOnMenuItemClickListener(menuItem -> {
            OpenWithDialogFragment fragment = OpenWithDialogFragment.getInstance(item.path);
            fragment.show(mFmActivity.getSupportFragmentManager(), OpenWithDialogFragment.TAG);
            return true;
        });
        menu.findItem(R.id.action_cut).setOnMenuItemClickListener(menuItem -> {
            // TODO: 21/11/22
            UIUtils.displayLongToast("Not implemented.");
            return false;
        });
        menu.findItem(R.id.action_copy).setOnMenuItemClickListener(menuItem -> {
            // TODO: 21/11/22
            UIUtils.displayLongToast("Not implemented.");
            return false;
        });
        menu.findItem(R.id.action_rename).setOnMenuItemClickListener(menuItem -> {
            RenameDialogFragment dialog = RenameDialogFragment.getInstance(item.path.getName(), (prefix, extension) -> {
                String displayName;
                if (!TextUtilsCompat.isEmpty(extension)) {
                    displayName = prefix + "." + extension;
                } else {
                    displayName = prefix;
                }
                if (item.path.renameTo(displayName)) {
                    UIUtils.displayShortToast(R.string.renamed_successfully);
                    mViewModel.reload();
                } else {
                    UIUtils.displayShortToast(R.string.failed);
                }
            });
            dialog.show(mFmActivity.getSupportFragmentManager(), RenameDialogFragment.TAG);
            return false;
        });
        menu.findItem(R.id.action_delete).setOnMenuItemClickListener(menuItem -> {
            new MaterialAlertDialogBuilder(mFmActivity)
                    .setTitle(mFmActivity.getString(R.string.delete_filename, item.path.getName()))
                    .setMessage(R.string.are_you_sure)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm_file_deletion, (dialog, which) -> {
                        if (item.path.delete()) {
                            UIUtils.displayShortToast(R.string.deleted_successfully);
                            mViewModel.reload();
                        } else {
                            UIUtils.displayShortToast(R.string.failed);
                        }
                    })
                    .show();
            return true;
        });
        menu.findItem(R.id.action_share).setOnMenuItemClickListener(menuItem -> {
            mViewModel.shareFiles(Collections.singletonList(item.path));
            return true;
        });
        boolean isVfs = mViewModel.getOptions().isVfs;
        menu.findItem(R.id.action_shortcut)
                // TODO: 31/5/23 Enable creating shortcuts for VFS
                .setEnabled(!isVfs)
                .setVisible(!isVfs)
                .setOnMenuItemClickListener(menuItem -> {
                    mViewModel.createShortcut(item);
                    return true;
                });
        menu.findItem(R.id.action_copy_path).setOnMenuItemClickListener(menuItem -> {
            String path = FmUtils.getDisplayablePath(item.path);
            ClipboardManager clipboard = (ClipboardManager) mFmActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("File path", path));
            UIUtils.displayShortToast(R.string.copied_to_clipboard);
            return true;
        });
        menu.findItem(R.id.action_properties).setOnMenuItemClickListener(menuItem -> {
            mViewModel.getDisplayPropertiesLiveData().setValue(item.path.getUri());
            return true;
        });
        popupMenu.show();
    }

    protected static class ViewHolder extends MultiSelectionView.ViewHolder {
        final MaterialCardView itemView;
        final ShapeableImageView icon;
        final ShapeableImageView symbolicLinkIcon;
        final MaterialButton action;
        final AppCompatTextView title;
        final AppCompatTextView subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = (MaterialCardView) itemView;
            icon = itemView.findViewById(android.R.id.icon);
            symbolicLinkIcon = itemView.findViewById(R.id.symolic_link_icon);
            action = itemView.findViewById(android.R.id.button1);
            title = itemView.findViewById(android.R.id.title);
            subtitle = itemView.findViewById(android.R.id.summary);
            action.setIconResource(io.github.muntashirakon.ui.R.drawable.ic_more_vert);
            itemView.findViewById(R.id.divider).setVisibility(View.GONE);
        }
    }
}
