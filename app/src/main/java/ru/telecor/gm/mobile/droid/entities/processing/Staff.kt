package ru.telecor.gm.mobile.droid.entities.processing

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose

class Staff (
    @SerializedName("firstName")
    @Expose
    var firstName: String? = null,

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("lastName")
    @Expose
    var lastName: String? = null,

    @SerializedName("middleName")
    @Expose
    var middleName: String? = null
)