import time
import pyrebase
import win32clipboard
from win10toast import ToastNotifier
from PIL import ImageGrab, Image
import io
from pynput.keyboard import Key, Listener

COMBINATION_UPLOAD_L = {Key.ctrl_l, Key.up}
COMBINATION_UPLOAD_R = {Key.ctrl_r, Key.up}
COMBINATION_DOWNLOAD_L = {Key.ctrl_l, Key.down}
COMBINATION_DOWNLOAD_R = {Key.ctrl_r, Key.down}
COMBINATION_QUIT = {Key.ctrl_l, Key.left, Key.right}
current = set()

firebaseConfig = {"apiKey": "AIzaSyBRb-uTCXVa6wqfZZ5LqVAWqsiT_V4VTF0",
                  "authDomain": "clipsync-31845.firebaseapp.com",
                  "databaseURL": "https://clipsync-31845.firebaseio.com",
                  "projectId": "clipsync-31845",
                  "storageBucket": "clipsync-31845.appspot.com",
                  "messagingSenderId": "748228670676",
                  "appId": "1:748228670676:web:9573a208acf5ceabf2f741"}

firebase = pyrebase.initialize_app(firebaseConfig)
db = firebase.database()
storage = firebase.storage()

toaster = ToastNotifier()
toaster.show_toast("ClipSync", "ClipSync started",
                  icon_path="ClipSync.ico", duration=5, threaded=True)


def on_press(key):
    print('{0} pressed'.format(key))
    if key in COMBINATION_UPLOAD_L or key in COMBINATION_UPLOAD_R:
        current.add(key)
        if all(k in current for k in COMBINATION_UPLOAD_L) ^ all(k in current for k in COMBINATION_UPLOAD_R):
            if win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_TEXT):
                win32clipboard.OpenClipboard()
                data = win32clipboard.GetClipboardData()
                db.child("clipboard").set(data)
                db.child("last_used").set("text")
                win32clipboard.CloseClipboard()

                toaster.show_toast("ClipSync", "Uploaded: " + data,
                                   icon_path="ClipSync.ico", duration=5,
                                   threaded=True)
            else:
                try:
                    img = ImageGrab.grabclipboard();
                    img_bytes = io.BytesIO()
                    img.save(img_bytes, format='PNG')
                    storage.child("image").put(img_bytes.getvalue())
                    db.child("last_used").set("image")

                    toaster.show_toast("ClipSync", "Uploaded Image",
                                       icon_path="ClipSync.ico",
                                       duration=5, threaded=True)
                except:
                    print("Unsupported file type")
                    toaster.show_toast("ClipSync", "Unsupported File Type",
                                       icon_path="ClipSync.ico",
                                       duration=5, threaded=True)


    elif key in COMBINATION_DOWNLOAD_L or key in COMBINATION_DOWNLOAD_R:
        current.add(key)
        if all(k in current for k in COMBINATION_DOWNLOAD_L) ^ all(k in current for k in COMBINATION_DOWNLOAD_R):

            if db.child("last_used").get().val() == "text":
                data = db.child("clipboard").get().val()
                win32clipboard.OpenClipboard()
                win32clipboard.EmptyClipboard()
                win32clipboard.SetClipboardText(data)
                win32clipboard.CloseClipboard()
                toaster.show_toast("ClipSync", "Copied: " + data,
                                   icon_path="ClipSync.ico", duration=5,
                                   threaded=True)

            elif db.child("last_used").get().val() == "image":
                storage.child("image").download("temp_image")
                image = Image.open("temp_image")
                output = io.BytesIO()
                image.convert("RGB").save(output, "BMP")
                data = output.getvalue()[14:]
                output.close()
                win32clipboard.OpenClipboard()
                win32clipboard.EmptyClipboard()
                win32clipboard.SetClipboardData(win32clipboard.CF_DIB, data)
                win32clipboard.CloseClipboard()
                print("copied")
                toaster.show_toast("ClipSync", "Copied Image",
                                   icon_path="ClipSync.ico", duration=5,
                                   threaded=True)


    elif key in COMBINATION_QUIT:
        current.add(key)
        if all(k in current for k in COMBINATION_QUIT):
            toaster.show_toast("ClipSync", "ClipSync ended",
                               icon_path="ClipSync.ico", duration=5,
                               threaded=True)
            while toaster.notification_active(): time.sleep(0.1)
            return False


def on_release(key):
    try:
        current.remove(key)
    except KeyError:
        pass


def getTheClipboardType():
    formats = []
    win32clipboard.OpenClipboard()
    lastFormat = 0
    while True:
        nextFormat = win32clipboard.EnumClipboardFormats(lastFormat)
        if 0 == nextFormat:
            # all done -- get out of the loop
            break
        else:
            formats.append(nextFormat)
            lastFormat = nextFormat
    win32clipboard.CloseClipboard()
    return formats


# Collect events until released
with Listener(
        on_press=on_press,
        on_release=on_release) as listener:
    listener.join()
