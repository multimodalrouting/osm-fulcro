(ns app.ui.framework7.components
  (:require
    [app.ui.framework7.factory-helpers :as h]
    ["framework7-react/components/accordion-content" :default AccordionContent]
    ["framework7-react/components/accordion-item" :default AccordionItem]
    ["framework7-react/components/accordion-toggle" :default AccordionToggle]
    ["framework7-react/components/accordion" :default Accordion]
    ["framework7-react/components/actions-button" :default ActionsButton]
    ["framework7-react/components/actions-group" :default ActionsGroup]
    ["framework7-react/components/actions-label" :default ActionsLabel]
    ["framework7-react/components/actions" :default Actions]
    ["framework7-react/components/app" :default App]
    ["framework7-react/components/appbar" :default Appbar]
    ["framework7-react/components/badge" :default Badge]
    ["framework7-react/components/block-footer" :default BlockFooter]
    ["framework7-react/components/block-header" :default BlockHeader]
    ["framework7-react/components/block-title" :default BlockTitle]
    ["framework7-react/components/block" :default Block]
    ["framework7-react/components/button" :default Button]
    ["framework7-react/components/card-content" :default CardContent]
    ["framework7-react/components/card-footer" :default CardFooter]
    ["framework7-react/components/card-header" :default CardHeader]
    ["framework7-react/components/card" :default Card]
    ["framework7-react/components/checkbox" :default Checkbox]
    ["framework7-react/components/chip" :default Chip]
    ["framework7-react/components/col" :default Col]
    ["framework7-react/components/fab-button" :default FabButton]
    ["framework7-react/components/fab-buttons" :default FabButtons]
    ["framework7-react/components/fab" :default Fab]
    ["framework7-react/components/gauge" :default Gauge]
    ["framework7-react/components/icon" :default Icon]
    ["framework7-react/components/input" :default Input]
    ["framework7-react/components/link" :default Link]
    ["framework7-react/components/list-button" :default ListButton]
    ["framework7-react/components/list-group" :default ListGroup]
    ["framework7-react/components/list-index" :default ListIndex]
    ["framework7-react/components/list-input" :default ListInput]
    ["framework7-react/components/list-item-cell" :default ListItemCell]
    ["framework7-react/components/list-item-content" :default ListItemContent]
    ["framework7-react/components/list-item-row" :default ListItemRow]
    ["framework7-react/components/list-item" :default ListItem]
    ["framework7-react/components/list" :default List]
    ["framework7-react/components/login-screen-title" :default LoginScreenTitle]
    ["framework7-react/components/login-screen" :default LoginScreen]
    ["framework7-react/components/menu-dropdown-item" :default MenuDropdownItem]
    ["framework7-react/components/menu-dropdown" :default MenuDropdown]
    ["framework7-react/components/menu-item" :default MenuItem]
    ["framework7-react/components/menu" :default Menu]
    ["framework7-react/components/message" :default Message]
    ["framework7-react/components/messagebar-attachment" :default MessagebarAttachment]
    ["framework7-react/components/messagebar-attachments" :default MessagebarAttachments]
    ["framework7-react/components/messagebar-sheet-image" :default MessagebarSheetImage]
    ["framework7-react/components/messagebar-sheet-item" :default MessagebarSheetItem]
    ["framework7-react/components/messagebar-sheet" :default MessagebarSheet]
    ["framework7-react/components/messagebar" :default Messagebar]
    ["framework7-react/components/messages-title" :default MessagesTitle]
    ["framework7-react/components/messages" :default Messages]
    ["framework7-react/components/nav-left" :default NavLeft]
    ["framework7-react/components/nav-right" :default NavRight]
    ["framework7-react/components/nav-title-large" :default NavTitleLarge]
    ["framework7-react/components/nav-title" :default NavTitle]
    ["framework7-react/components/navbar" :default Navbar]
    ["framework7-react/components/page-content" :default PageContent]
    ["framework7-react/components/page" :default Page]
    ["framework7-react/components/panel" :default Panel]
    ["framework7-react/components/photo-browser" :default PhotoBrowser]
    ["framework7-react/components/popover" :default Popover]
    ["framework7-react/components/popup" :default Popup]
    ["framework7-react/components/preloader" :default Preloader]
    ["framework7-react/components/progressbar" :default Progressbar]
    ["framework7-react/components/radio" :default Radio]
    ["framework7-react/components/range" :default Range]
    ["framework7-react/components/routable-modals" :default RoutableModals]
    ["framework7-react/components/row" :default Row]
    ["framework7-react/components/searchbar" :default Searchbar]
    ["framework7-react/components/segmented" :default Segmented]
    ["framework7-react/components/sheet" :default Sheet]
    ["framework7-react/components/skeleton-block" :default SkeletonBlock]
    ["framework7-react/components/skeleton-text" :default SkeletonText]
    ["framework7-react/components/stepper" :default Stepper]
    ["framework7-react/components/subnavbar" :default Subnavbar]
    ["framework7-react/components/swipeout-actions" :default SwipeoutActions]
    ["framework7-react/components/swipeout-button" :default SwipeoutButton]
    ["framework7-react/components/swiper-slide" :default SwiperSlide]
    ["framework7-react/components/swiper" :default Swiper]
    ["framework7-react/components/tab" :default Tab]
    ["framework7-react/components/tabs" :default Tabs]
    ["framework7-react/components/toggle" :default Toggle]
    ["framework7-react/components/toolbar" :default Toolbar]
    ["framework7-react/components/treeview-item" :default TreeviewItem]
    ["framework7-react/components/treeview" :default Treeview]
    ["framework7-react/components/view" :default View]
    ["framework7-react/components/views" :default Views]
    ))


(def f7-accordion-content (h/factory-apply AccordionContent))
(def f7-accordion-item (h/factory-apply AccordionItem))
(def f7-accordion-toggle (h/factory-apply AccordionToggle))
(def f7-accordion (h/factory-apply Accordion))
(def f7-actions-button (h/factory-apply ActionsButton))
(def f7-actions-group (h/factory-apply ActionsGroup))
(def f7-actions-label (h/factory-apply ActionsLabel))
(def f7-actions (h/factory-apply Actions))
(def f7-app (h/factory-apply App))
(def f7-appbar (h/factory-apply Appbar))
(def f7-badge (h/factory-apply Badge))
(def f7-block-footer (h/factory-apply BlockFooter))
(def f7-block-header (h/factory-apply BlockHeader))
(def f7-block-title (h/factory-apply BlockTitle))
(def f7-block (h/factory-apply Block))
(def f7-button (h/factory-apply Button))
(def f7-card-content (h/factory-apply CardContent))
(def f7-card-footer (h/factory-apply CardFooter))
(def f7-card-header (h/factory-apply CardHeader))
(def f7-card (h/factory-apply Card))
(def f7-checkbox (h/factory-apply Checkbox))
(def f7-chip (h/factory-apply Chip))
(def f7-col (h/factory-apply Col))
(def f7-fab-button (h/factory-apply FabButton))
(def f7-fab-buttons (h/factory-apply FabButtons))
(def f7-fab (h/factory-apply Fab))
(def f7-gauge (h/factory-apply Gauge))
(def f7-icon (h/factory-apply Icon))
(def f7-input (h/factory-apply Input))
(def f7-link (h/factory-apply Link))
(def f7-list-button (h/factory-apply ListButton))
(def f7-list-group (h/factory-apply ListGroup))
(def f7-list-index (h/factory-apply ListIndex))
(def f7-list-input (h/factory-apply ListInput))
(def f7-list-item-cell (h/factory-apply ListItemCell))
(def f7-list-item-content (h/factory-apply ListItemContent))
(def f7-list-item-row (h/factory-apply ListItemRow))
(def f7-list-item (h/factory-apply ListItem))
(def f7-list (h/factory-apply List))
(def f7-login-screen-title (h/factory-apply LoginScreenTitle))
(def f7-login-screen (h/factory-apply LoginScreen))
(def f7-menu-dropdown-item (h/factory-apply MenuDropdownItem))
(def f7-menu-dropdown (h/factory-apply MenuDropdown))
(def f7-menu-item (h/factory-apply MenuItem))
(def f7-menu (h/factory-apply Menu))
(def f7-message (h/factory-apply Message))
(def f7-messagebar-attachment (h/factory-apply MessagebarAttachment))
(def f7-messagebar-attachments (h/factory-apply MessagebarAttachments))
(def f7-messagebar-sheet-image (h/factory-apply MessagebarSheetImage))
(def f7-messagebar-sheet-item (h/factory-apply MessagebarSheetItem))
(def f7-messagebar-sheet (h/factory-apply MessagebarSheet))
(def f7-messagebar (h/factory-apply Messagebar))
(def f7-messages-title (h/factory-apply MessagesTitle))
(def f7-messages (h/factory-apply Messages))
(def f7-nav-left (h/factory-apply NavLeft))
(def f7-nav-right (h/factory-apply NavRight))
(def f7-nav-title-large (h/factory-apply NavTitleLarge))
(def f7-nav-title (h/factory-apply NavTitle))
(def f7-navbar (h/factory-apply Navbar))
(def f7-page-content (h/factory-apply PageContent))
(def f7-page (h/factory-apply Page))
(def f7-panel (h/factory-apply Panel))
(def f7-photo-browser (h/factory-apply PhotoBrowser))
(def f7-popover (h/factory-apply Popover))
(def f7-popup (h/factory-apply Popup))
(def f7-preloader (h/factory-apply Preloader))
(def f7-progressbar (h/factory-apply Progressbar))
(def f7-radio (h/factory-apply Radio))
(def f7-range (h/factory-apply Range))
(def f7-routable-modals (h/factory-apply RoutableModals))
(def f7-row (h/factory-apply Row))
(def f7-searchbar (h/factory-apply Searchbar))
(def f7-segmented (h/factory-apply Segmented))
(def f7-sheet (h/factory-apply Sheet))
(def f7-skeleton-block (h/factory-apply SkeletonBlock))
(def f7-skeleton-text (h/factory-apply SkeletonText))
(def f7-stepper (h/factory-apply Stepper))
(def f7-subnavbar (h/factory-apply Subnavbar))
(def f7-swipeout-actions (h/factory-apply SwipeoutActions))
(def f7-swipeout-button (h/factory-apply SwipeoutButton))
(def f7-swiper-slide (h/factory-apply SwiperSlide))
(def f7-swiper (h/factory-apply Swiper))
(def f7-tab (h/factory-apply Tab))
(def f7-tabs (h/factory-apply Tabs))
(def f7-toggle (h/factory-apply Toggle))
(def f7-toolbar (h/factory-apply Toolbar))
(def f7-treeview-item (h/factory-apply TreeviewItem))
(def f7-treeview (h/factory-apply Treeview))
(def f7-view (h/factory-apply View))
(def f7-views (h/factory-apply Views))

