package org.rsmod.content.raids.tob

import org.rsmod.map.CoordGrid

fun resolve(coord: CoordGrid): CoordGrid = coord

fun resolve(session: Any?, coord: CoordGrid): CoordGrid = coord
