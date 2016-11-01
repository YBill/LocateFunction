package com.bill.locatefunction;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bill on 2016/10/31.
 */
public class MoreLocateAdapter extends RecyclerView.Adapter<MoreLocateAdapter.LocateViewHolder> {

    private LayoutInflater inflater;
    private OnItemClickLitener onItemClickLitener;
    private List<POIEntity> locateList = new ArrayList<>();

    public MoreLocateAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setLocateList(List<POIEntity> locateList) {
        this.locateList.clear();
        this.locateList.addAll(locateList);
        this.notifyDataSetChanged();
    }

    @Override
    public LocateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.adapter_more_locate, parent, false);
        LocateViewHolder myViewHolder = new LocateViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(final LocateViewHolder holder, int position) {
        POIEntity entity = locateList.get(position);
        holder.titleText.setText(entity.title);
        if(TextUtils.isEmpty(entity.content)){
            holder.contentText.setVisibility(View.GONE);
        } else {
            holder.contentText.setVisibility(View.VISIBLE);
            holder.contentText.setText(entity.content);
        }
        if (entity.isSelect)
            holder.selectView.setVisibility(View.VISIBLE);
        else
            holder.selectView.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickLitener != null) {
                    int position = holder.getLayoutPosition();
                    onItemClickLitener.onItemClick(holder.itemView, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return locateList.size();
    }

    public POIEntity getItem(int position) {
        return locateList.get(position);
    }

    public interface OnItemClickLitener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickLitener(OnItemClickLitener onItemClickLitener) {
        this.onItemClickLitener = onItemClickLitener;
    }

    class LocateViewHolder extends RecyclerView.ViewHolder {

        TextView titleText;
        TextView contentText;
        View selectView;

        public LocateViewHolder(View itemView) {
            super(itemView);
            titleText = (TextView) itemView.findViewById(R.id.tv_title);
            contentText = (TextView) itemView.findViewById(R.id.tv_content);
            selectView = itemView.findViewById(R.id.iv_select);
        }
    }
}
