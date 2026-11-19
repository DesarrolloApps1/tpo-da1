package com.takeabreak.domain.model

// Permisos que usa la app: acceso a uso es obligatorio, los otros recomendados.
// BATTERY_OPTIMIZATION: la app está excluida de la optimización de batería, para que el aviso llegue antes
enum class AppPermission { USAGE_ACCESS, NOTIFICATIONS, LOCATION, BATTERY_OPTIMIZATION }
