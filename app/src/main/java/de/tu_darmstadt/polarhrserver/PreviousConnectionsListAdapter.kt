package de.tu_darmstadt.polarhrserver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListAdapter
import android.widget.TextView


data class ConnectionData(val name: String, val onStartSendData: (view: View) -> Unit)

/**
 * Created by Marcel Zickler on 27.11.2020.
 */
class PreviousConnectionsListAdapter(
    var data: List<ConnectionData>,
    private val layoutInflater: LayoutInflater
) : BaseAdapter() {

    override fun getCount(): Int = data.count()

    override fun getItem(position: Int) = data[position];

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var cView = convertView;

        if (cView == null) {
            cView = layoutInflater.inflate(R.layout.previous_connections_list_item, parent, false)
        }

        cView?.findViewById<TextView>(R.id.tv_connection_name)?.text = getItem(position).name
        cView?.findViewById<Button>(R.id.btn_connect_previous)
            ?.setOnClickListener(getItem(position).onStartSendData)

        return cView!!
    }

}