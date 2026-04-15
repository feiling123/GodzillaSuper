import base64
import sys
from Crypto.Cipher import AES

def pkcs7unpadding(text):
    length = len(text)
    padding_length = ord(text[-1])
    if padding_length > length:
        raise ValueError("Padding length is larger than the text length")
    return text[0:length - padding_length]


def decrypt(key, enc_passwords):
    passwords = []
    key_bytes = bytes.fromhex(key)
    print("Starting decryption process...")
    for ip, usr, enc_password in enc_passwords:
        try:
            content = base64.b64decode(enc_password)
            iv_bytes = content[:16]
            enc_password_bytes = content[16:]
            cipher = AES.new(key_bytes, AES.MODE_CBC, iv_bytes)
            password_bytes = cipher.decrypt(enc_password_bytes)
            password = password_bytes.decode('utf-8', errors='ignore')
            password = pkcs7unpadding(password)
            line = '{}:{}:{}'.format(ip, usr, password)
            print("Decrypted:", line) 
            passwords.append(line)
        except Exception as e:
            print("[ERROR] Decrypting password for {}:{} failed: {}".format(ip, usr, e))
    return passwords


def save_decrypt_password(path, passwords):
    print("Saving decrypted passwords to file...")
    data = '\n'.join(passwords)
    with open(path, 'w') as file:
        file.write(data)


def get_encrypt_password(path):
    encrypt_passwords = []
    print("Reading encrypted passwords from file...")
    with open(path) as file:
        for idx, line in enumerate(file):
            if idx <= 1:
                continue
            try:
                line = [el.strip() for el in line.split("|")]
                ip = line[0]
                usr = line[1]
                pw = line[2]
                encrypt_password = pw.strip('*').strip()
                encrypt_passwords.append((ip, usr, encrypt_password))
            except IndexError as e:
                print("[ERROR] Invalid line format in encrypted passwords file: {}".format(line))
                return encrypt_passwords  
    return encrypt_passwords


def get_key(path):
    print("Reading key from file...")
    with open(path) as file:
        key = file.read().strip()
        return key


def main():
    if len(sys.argv) != 4:
        print("Usage: python vhost_password_decrypt.py <key_file> <encrypted_password_file> <output_file>")
        exit(1)
    
    key = get_key(sys.argv[1])  
    encrypt_passwords = get_encrypt_password(sys.argv[2]) 
    save_path = sys.argv[3] 
    passwords = decrypt(key, encrypt_passwords)  
    save_decrypt_password(save_path, passwords) 


if __name__ == '__main__':
    main()
