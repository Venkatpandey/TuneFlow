# TV navigation and Back behavior

TuneFlow treats remote Back as an escape through one visible layer at a time.

## Escape order

1. Close transient UI owned by the current screen, such as search editing, the Now Playing queue, or playlist detail.
2. Pop one nested destination, such as Album, Artist, Home Category, or Now Playing.
3. Return a root Albums, Playlists, or Search section to Home.
4. On Home, show the exit prompt. A second Back press inside the confirmation window exits the app.

Confirmed app exit deliberately stops playback, clears the player, stops the playback service, and removes the task.

## Destination rules

- Selecting an item pushes its detail destination onto the current stack.
- Selecting a top-level navigation-rail section resets the stack to that section root.
- Now Playing is pushed over the current destination, so Back returns to the exact underlying screen.
- Back from nested Artist to Album navigation returns to Artist before returning to its source screen.
- Back from playlist detail closes the detail panel before leaving Playlists.

## Focus rules

- Closing transient UI restores focus to the control or item that opened it.
- Popping detail restores the originating album, artist, playlist, or Home category action.
- Focus restoration scrolls the containing list or grid before requesting focus.
- If no restoration target exists, each screen uses its normal initial-focus behavior.

## Incremental lists

Home loads each library rail when its row enters the viewport. Rails start with five items; the D-pad-accessible **Load more** card appends five more without resetting the row's scroll state or replacing existing item keys. When the last page removes that card, focus moves to **Show all**. Loading failures leave existing cards available and expose a **Retry** action.

Playlist detail starts with 50 displayed tracks and appends another 50 when scrolling within ten rows of the displayed end. Track keys include their occurrence index so repeated songs remain separate rows. Play, Shuffle, and track selection always use the full playlist, not the displayed prefix. Playlist-list artwork is requested only for visible rows, in batches of at most four.

Albums use Subsonic's size/offset pagination. Artists, favorites, playlist summaries, and playlist detail use whole-response endpoints; their incremental display does not reduce the size of those responses.

On a remote device, check that an existing card keeps focus while a page appends, **Load more** remains focused until exhaustion, and Back restores an item beyond the first page. For a playlist with over 100 tracks, scroll across both page boundaries, select a repeated song occurrence, and confirm playback continues beyond the displayed prefix.

## Test expectations

Navigation changes should cover the affected stack transition with unit tests. Transient screen changes should also cover their escape decision or cancellation behavior. Remote-device verification should confirm that focus remains visible after every Back press.
