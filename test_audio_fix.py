#!/usr/bin/env python3
"""
测试掏耳朵音频修复的验证脚本
"""

import json
import requests
import sys

def test_github_file():
    """测试 GitHub 原始文件"""
    print("🔍 测试 GitHub 原始文件...")
    try:
        response = requests.get("https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/sounds_remote.json")
        response.raise_for_status()
        data = response.json()
        
        ear_cleaning_items = [item for item in data['sounds'] if 'ear-cleaning' in item.get('id', '')]
        
        if len(ear_cleaning_items) != 2:
            print(f"❌ 期望找到2个掏耳朵音频，实际找到{len(ear_cleaning_items)}个")
            return False
            
        for item in ear_cleaning_items:
            if '\n' in item['remoteUrl']:
                print(f"❌ {item['id']}: URL 包含换行符")
                return False
            print(f"✅ {item['id']}: URL 格式正确")
            
        return True
    except Exception as e:
        print(f"❌ GitHub 文件测试失败: {e}")
        return False

def test_audio_accessibility():
    """测试音频文件可访问性"""
    print("\n🔍 测试音频文件可访问性...")
    urls = [
        "https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/things/ear-cleaning-1.mp3",
        "https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/things/ear-cleaning-2.mp3"
    ]
    
    for i, url in enumerate(urls, 1):
        try:
            response = requests.head(url)
            if response.status_code == 200:
                print(f"✅ 掏耳朵{i}: 音频文件可访问 ({response.headers.get('content-length', 'unknown size')} bytes)")
            else:
                print(f"❌ 掏耳朵{i}: HTTP {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ 掏耳朵{i}: 访问失败 - {e}")
            return False
    
    return True

def test_cdn_update():
    """测试 CDN 更新状态"""
    print("\n🔍 测试 CDN 更新状态...")
    try:
        response = requests.get("https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/sounds_remote.json")
        response.raise_for_status()
        data = response.json()
        
        ear_cleaning_items = [item for item in data['sounds'] if 'ear-cleaning' in item.get('id', '')]
        
        if len(ear_cleaning_items) == 2:
            print("✅ CDN 已更新，包含掏耳朵音频")
            for item in ear_cleaning_items:
                if '\n' not in item['remoteUrl']:
                    print(f"✅ {item['id']}: CDN URL 格式正确")
                else:
                    print(f"❌ {item['id']}: CDN URL 仍有换行符")
                    return False
            return True
        else:
            print(f"⚠️ CDN 尚未更新，当前包含 {len(ear_cleaning_items)} 个掏耳朵音频")
            print("   CDN 可能需要更多时间同步，建议稍后再试")
            return False
            
    except Exception as e:
        print(f"❌ CDN 测试失败: {e}")
        return False

def main():
    print("🎧 XMSLEEP 掏耳朵音频修复验证")
    print("=" * 50)
    
    tests = [
        ("GitHub 文件格式", test_github_file),
        ("音频文件可访问性", test_audio_accessibility),
        ("CDN 更新状态", test_cdn_update)
    ]
    
    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, result))
        except Exception as e:
            print(f"❌ {name} 测试异常: {e}")
            results.append((name, False))
    
    print("\n" + "=" * 50)
    print("📊 测试结果总结:")
    
    for name, result in results:
        status = "✅ 通过" if result else "❌ 失败"
        print(f"  {name}: {status}")
    
    # 如果 GitHub 和音频文件测试通过，说明修复成功
    github_ok = results[0][1]
    audio_ok = results[1][1]
    
    if github_ok and audio_ok:
        print("\n🎉 修复成功！")
        print("   - GitHub 上的 sounds_remote.json 文件已修复")
        print("   - 掏耳朵音频文件可正常访问")
        print("   - 应用现在应该能正确显示掏耳朵音频卡片")
        
        if not results[2][1]:
            print("\n⚠️ 注意:")
            print("   CDN 可能需要一些时间来更新（通常几分钟到几小时）")
            print("   如果应用中仍未显示，请稍后重试或重启应用")
    else:
        print("\n❌ 修复未完全成功，请检查上述失败项目")
        sys.exit(1)

if __name__ == "__main__":
    main()
