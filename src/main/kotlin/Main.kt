// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.commuto.interfacedesktop.DaggerCommutoApplicationFactory

fun main() {
    val appFactory = DaggerCommutoApplicationFactory.create()
    val app = appFactory.commutoApplicationFactory()
    app.start()
}