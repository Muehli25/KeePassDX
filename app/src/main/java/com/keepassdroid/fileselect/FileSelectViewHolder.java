/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.fileselect;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

class FileSelectViewHolder extends RecyclerView.ViewHolder {

    View fileContainer;
    TextView fileName;
    ImageView fileInformation;

    FileSelectViewHolder(View itemView) {
        super(itemView);
        fileContainer = itemView.findViewById(R.id.file_container);
        fileName = (TextView) itemView.findViewById(R.id.file_filename);
        fileInformation = (ImageView) itemView.findViewById(R.id.file_information);
    }
}