package com.example.iotsecurity;

import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


/**
 * Product에 대한 정보를 layout에 적용
 *
 * ver. 2020.9.27 : Product에 대한 데이터를 recyclerview adapter 형식에 맞게 viewHolder 재정의
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.myViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    // 객체 참조 저장
    private OnItemClickListener itemListener = null;

    // OnItemClickListener 객체 참조를 어댑터에 전달
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemListener = listener;
    }

    /**
     * recycler view에 받아온 데이터를 채워 넣음
     */
    class myViewHolder extends RecyclerView.ViewHolder {
        TextView score, name, provider, category;
        ImageButton deleteButton;

        public final View layout;

        //
        /**
         * 각 Product에 해당하는 itemView마다 Click Listner를 설정해 둠.
         * OnClick ->
         * @param itemView
         */
        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            score = itemView.findViewById(R.id.scoreView);
            name = itemView.findViewById(R.id.name);
            provider = itemView.findViewById(R.id.provider);
            category = itemView.findViewById(R.id.category);
            deleteButton = itemView.findViewById(R.id.delete);


            layout = itemView;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메소드 호출
                        if (itemListener != null) {
                            itemListener.onItemClick(v, pos);
                        }
                    }
                }
            });
        }

        /**
         * 받아온 product 객체를 view에 대입
         * @param item
         */
        public void setItem(Product item) {
            final Product tmp = item;

            score.setText(String.valueOf(tmp.score));
            name.setText(tmp.name);
            provider.setText(tmp.provider);
            category.setText(tmp.category);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("Products");

                    // 하드 코딩
                    if(tmp.category.equals("전구")) {
                        String lightNum = tmp.name.replaceAll("[^0-9]", "");
                        mDatabase.child(lightNum).removeValue();
                    } else if (tmp.category.equals("체중계")) {
                        mDatabase.child("3").removeValue();
                    }
                }
            });
        }
    }

    ArrayList<Product> products = new ArrayList<Product>();

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.product_item, parent, false);
        myViewHolder mViewHolder = new myViewHolder(itemView);

        return mViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Product item = products.get(position);
        holder.setItem(item);

    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void addItem(Product item) {
        products.add(item);
    }

    public void setItems(ArrayList<Product> items) {
        this.products = items;
    }

    public Product getItem(int position) {
        return products.get(position);
    }

    public void clearItems() {
        this.products.clear();
    }


}
