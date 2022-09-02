package ru.telecor.gm.mobile.droid.entities.processing

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose
import ru.telecor.gm.mobile.droid.entities.db.PhotoProcessingForApi
import ru.telecor.gm.mobile.droid.entities.processing.ContainerStatus

class ContainerStatusGroup (
    @SerializedName("containerStatuses")
    @Expose
    var containerStatuses: List<ContainerStatus>? = null,

    @SerializedName("createTime")
    @Expose
    var createTime: String? = null,

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("photos")
    @Expose
    var photos: List<PhotoProcessingForApi>? = null,

    @SerializedName("volume")
    @Expose
    var volume: Int? = null,

    @SerializedName("weight")
    @Expose
    var weight: Int? = null
)