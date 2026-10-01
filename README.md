# common-ui-list

[![codecov](https://codecov.io/gh/storytellerF/common-ui-list/graph/badge.svg?token=5IK0PP6G9G)](https://codecov.io/gh/storytellerF/common-ui-list)


ViewHolder 不再缓存 bind 时的 item holder。`bindData` 使用传入的数据；点击等事件通过
`itemHolderOrNull` 获取当前 `bindingAdapterPosition` 对应的数据，无效位置或 Paging
占位返回 null。自定义 adapter 需实现 `ItemHolderProvider`，使用 adapter 内的相对位置读取数据。

`@BindClickEvent` / `@BindLongClickEvent` 回调使用 `position: Int`（或 `index: Int`）
接收点击时的 `bindingAdapterPosition`，不再接收 item holder。位置无效时不调用回调。
接收者可通过对应 adapter 的 `getItemHolder(position)` 查询数据。
